package com.fluidbpm.fluidwebkit.backing.bean.workspace.uq;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean.ColumnModel;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview.WebKitViewContentModelBean;
import com.fluidbpm.fluidwebkit.ds.FluidClientDS;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.field.MultiChoice;
import com.fluidbpm.program.api.vo.userquery.UserQuery;
import com.fluidbpm.program.api.vo.webkit.userquery.WebKitUserQuery;
import com.fluidbpm.ws.client.v1.form.FormFieldClient;
import org.junit.Test;

import javax.faces.model.SelectItem;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TestUserQueryFormTypeInput {
    private static final String SECTION = "User Query - Instances by Form Type";

    private static class TopBarBean extends WebKitUserQueryTopBarBean {
        TopBarBean(ContentViewUQ view) { this.contentView = view; }
    }

    private static class FieldClient extends FormFieldClient {
        int requests;
        boolean fail;

        FieldClient() { super(null, null); }

        @Override public Field getFieldByName(String name) {
            assertNotNull("Only recognised system fields may bypass the visible-field cache", ContentViewUQ.SystemField.queryFieldName(name));
            requests++;
            if (fail) throw new IllegalStateException("Field service unavailable");
            MultiChoice choices = new MultiChoice();
            choices.setAvailableMultiChoices(Arrays.asList("Organisation", "HSM Host"));
            Field.Type type = name.contains("Date") ? Field.Type.DateTime :
                    name.equals("Email Unique Identifier") ? Field.Type.Decimal :
                    Arrays.asList("Form Type", "Form Flow State", "Current User").contains(name) ? Field.Type.MultipleChoice : Field.Type.Text;
            Field field = new Field(name, type == Field.Type.MultipleChoice ? choices : null, type);
            field.setId(-1L);
            field.setTypeMetaData("Plain");
            field.setFieldDescription("The Alias of the Form Container Type.");
            return field;
        }
    }

    private static class Access extends WebKitAccessBean {
        final FieldClient client = new FieldClient();
        final UserQuery query = new UserQuery(7L, "Instances by Form Type");
        final FluidClientDS config = new FluidClientDS() {
            @Override public FormFieldClient getFormFieldClient() { return client; }
        };

        Access(String... rules) throws Exception {
            query.setRules(Arrays.asList(rules));
            java.lang.reflect.Field cache = WebKitAccessBean.class.getDeclaredField("fieldsForViewing");
            cache.setAccessible(true);
            cache.set(this, Collections.singletonMap("Organisation",
                    Collections.singletonList(new Field("Alias", "", Field.Type.Text))));
        }

        @Override public FluidClientDS getFluidClientDSConfig() { return config; }
        @Override public UserQuery fetchUserQueryWithNameUsingConfigUser(String name) { return query; }
        @Override public List<Field> getFormFieldsUserHasVisibilityFor(UserQuery query) {
            return Collections.singletonList(getFieldBy("Alias"));
        }
    }

    private ContentViewUQ view(Access access) {
        WebKitUserQuery settings = new WebKitUserQuery();
        settings.setUserQuery(access.query);
        settings.setShowColumnFormType(false);
        ContentViewUQ view = new ContentViewUQ(null, SECTION, settings,
                new WebKitViewContentModelBean(), access);
        view.mapColumnModel();
        return view;
    }

    @Test public void formTypeInputAndChoicesExistWithoutACachedFieldOrVisibleResultColumn() throws Exception {
        Access access = new Access("[Form Type] = INPUT_VALUE");
        assertNull(access.getFieldBy("Form Type"));
        ContentViewUQ view = view(access);
        List<ColumnModel> inputs = view.getColumnModelsFilterable().get(SECTION);
        assertEquals(1, inputs.size());
        assertEquals("Form Type", inputs.get(0).getFluidFieldName());
        assertEquals(Field.Type.MultipleChoice, inputs.get(0).getFluidFieldColumnType());
        assertEquals("The Alias of the Form Container Type.", inputs.get(0).getFluidFieldDescription());
        assertTrue(view.getFilterBySelectItemMap().get(SECTION).containsKey("Form Type"));
        assertSame(inputs, view.getColumnModelsFilterable().get(SECTION));
        assertEquals(1, access.client.requests);

        List<SelectItem> choices = view.getPossibleCombinationsMapAsSelectItemsFor(SECTION, "Form Type");
        assertEquals(Arrays.asList("HSM Host", "Organisation"), choices.stream()
                .map(SelectItem::getValue).collect(Collectors.toList()));

        view.getFilterBySelectItemMap().get(SECTION).put("Form Type", new String[]{"Organisation"});
        WebKitUserQueryTopBarBean bean = new TopBarBean(view);
        List<Field> submitted = bean.getInputFieldValues();
        assertEquals(1, submitted.size());
        assertEquals("Form Type", submitted.get(0).getFieldName());
        assertEquals(Collections.singletonList("Organisation"),
                submitted.get(0).getFieldValueAsMultiChoice().getSelectedMultiChoices());
    }

    @Test public void mixedInputsKeepTheirOrderWithoutFetchingHiddenCustomFields() throws Exception {
        Access access = new Access("[Alias] = INPUT_VALUE", "[Form Type] = INPUT_VALUE",
                "[Hidden Custom Field] = INPUT_VALUE");
        List<ColumnModel> inputs = view(access).getColumnModelsFilterable().get(SECTION);
        assertEquals(Arrays.asList("Alias", "Form Type"), inputs.stream()
                .map(ColumnModel::getFluidFieldName).collect(Collectors.toList()));
        assertEquals(1, access.client.requests);
        assertNull(access.getFieldBy("Form Type"));
    }

    @Test public void staticFormTypeRuleNeedsNoInputOrSystemFieldLookup() throws Exception {
        Access access = new Access("[Form Type] = 'Organisation'");
        assertTrue(view(access).getColumnModelsFilterable().get(SECTION).isEmpty());
        assertEquals(0, access.client.requests);
    }

    @Test public void failedSystemLookupCanBeRetriedWithoutCachingAnEmptyInputList() throws Exception {
        Access access = new Access("[Form Type] = INPUT_VALUE");
        ContentViewUQ view = view(access);
        access.client.fail = true;
        try {
            view.getColumnModelsFilterable();
            fail("The lookup failure must be reported");
        } catch (IllegalStateException expected) {
            assertEquals("Field service unavailable", expected.getMessage());
        }
        access.client.fail = false;
        assertEquals(1, view.getColumnModelsFilterable().get(SECTION).size());
    }

    @Test public void everySystemFieldHasAnInputWithoutBeingAResultColumn() throws Exception {
        List<String> names = Arrays.asList("Date Created", "Date Last Updated", "Form Type", "Form Flow State",
                "Title", "Current User", "Email From Address", "Email To Address", "Email Subject",
                "Email Sent Date", "Email Received Date", "Email Unique Identifier");
        Access access = new Access(names.stream().map(name -> "[" + name + "] = INPUT_VALUE").toArray(String[]::new));
        ContentViewUQ view = view(access);
        assertEquals(names, view.getColumnModelsFilterable().get(SECTION).stream()
                .map(ColumnModel::getFluidFieldName).collect(Collectors.toList()));
        assertEquals(12, access.client.requests);
        view.getFilterByTextValueMap().get(SECTION).put("Email Subject", "Invoice");
        view.getFilterBySelectItemMap().get(SECTION).put("Current User", new String[]{"alice"});
        view.getFilterByDateValueMap().get(SECTION).put("Email Sent Date", new Date(1700000000000L));
        view.getFilterByDecimalValueMap().get(SECTION).put("Email Unique Identifier", 0.0);
        List<Field> submitted = new TopBarBean(view).getInputFieldValues();
        assertEquals(4, submitted.size());
        assertEquals(0.0, submitted.stream().filter(field -> field.getFieldName().equals("Email Unique Identifier"))
                .findFirst().get().getFieldValueAsDouble(), 0.0);
        assertEquals(new Date(1700000000000L), submitted.stream().filter(field -> field.getFieldName().equals("Email Sent Date"))
                .findFirst().get().getFieldValue());
        assertEquals("Invoice", view.getFilterByTextValueMap().get(SECTION).get("Email Subject"));
        assertEquals(12, access.client.requests);
    }

    @Test public void dateRangesUseBothValuesAndKeepStaticBoundsOutOfTheInputs() throws Exception {
        Access access = new Access("[Date Created] BETWEEN INPUT_VALUE AND INPUT_VALUE",
                "[Date Last Updated] BETWEEN INPUT_VALUE AND '1800000000000'",
                "[Email Sent Date] BETWEEN '1600000000000' AND INPUT_VALUE");
        ContentViewUQ view = view(access);
        List<ColumnModel> columns = view.getColumnModelsFilterable().get(SECTION);
        assertEquals(3, columns.size());
        assertFalse(((UserQueryInputColumn) columns.get(1)).isToInput());
        assertFalse(((UserQueryInputColumn) columns.get(2)).isFromInput());
        assertTrue(new TopBarBean(view).getInputFieldValues().isEmpty());
        view.getFilterByDateValueMap().get(SECTION).put("Date Created", new Date(1700000000000L));
        try { new TopBarBean(view).getInputFieldValues(); fail("Partial range accepted"); }
        catch (com.fluidbpm.fluidwebkit.exception.ClientDashboardException expected) { }
        view.getRangeToDates().get(SECTION).put("Date Created", new Date(1750000000000L));
        view.getFilterByDateValueMap().get(SECTION).put("Date Last Updated", new Date(1700000000000L));
        view.getRangeToDates().get(SECTION).put("Email Sent Date", new Date(1750000000000L));
        List<Field> submitted = new TopBarBean(view).getInputFieldValues();
        assertEquals(Arrays.asList("1700000000000|1750000000000", "1700000000000|", "|1750000000000"),
                submitted.stream().map(field -> new Field(field.toJsonObject()).getFieldValue()).collect(Collectors.toList()));
    }

    @Test public void numericRangesAllowZeroAndNegativeValues() throws Exception {
        Access access = new Access("[Email Unique Identifier] BETWEEN INPUT_VALUE AND INPUT_VALUE");
        ContentViewUQ view = view(access);
        view.getColumnModelsFilterable();
        view.getFilterByDecimalValueMap().get(SECTION).put("Email Unique Identifier", -1.0);
        view.getRangeToDecimals().get(SECTION).put("Email Unique Identifier", 0.0);
        assertEquals("-1.0|0.0", new TopBarBean(view).getInputFieldValues().get(0).getFieldValue());
    }
}
