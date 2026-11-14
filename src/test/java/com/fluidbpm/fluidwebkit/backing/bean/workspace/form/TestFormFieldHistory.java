package com.fluidbpm.fluidwebkit.backing.bean.workspace.form;

import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.program.api.vo.form.Form;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static com.fluidbpm.fluidwebkit.backing.bean.workspace.form.WebKitOpenFormFieldHistoryConversationBean.*;

public class TestFormFieldHistory {
    private Field field(Object value) {
        Field field = new Field();
        field.setFieldName("Organisation Name");
        field.setTypeAsEnum(Field.Type.Text);
        field.setFieldValue(value);
        return field;
    }

    @Test public void initialValuesSurviveWithoutPreviousSnapshot() throws Exception {
        List<FlatFieldHistory> rows = buildGeneralHistory(
                Collections.singletonMap(1000L, Arrays.asList(field("Acme"))),
                Collections.singletonMap(1000L, "creator"));
        assertEquals(1, rows.size());
        assertNull(rows.get(0).getPriorField());
        assertEquals("N/A", rows.get(0).getDisplayValueForTimeDiff());
        WebKitOpenFormFieldHistoryConversationBean bean = new WebKitOpenFormFieldHistoryConversationBean();
        java.lang.reflect.Field general = bean.getClass().getDeclaredField("generalHistory");
        general.setAccessible(true);
        general.set(bean, true);
        bean.setFormFieldHistoriesFlat(rows);
        assertEquals(1, bean.getFormFieldHistoriesFlatModifyOnly().size());
    }

    @Test public void editsClearsAndRevertsUseImmediatePreviousValuesAndUsers() {
        Map<Long, List<Field>> snapshots = new HashMap<>();
        snapshots.put(1000L, Arrays.asList(field("A")));
        snapshots.put(2000L, Arrays.asList(field("B")));
        snapshots.put(3000L, Arrays.asList(field("B")));
        snapshots.put(4000L, Arrays.asList(field(null)));
        snapshots.put(5000L, Arrays.asList(field("A")));
        Map<Long, String> users = new HashMap<>();
        for (long i = 1; i <= 5; i++) users.put(i * 1000, "user" + i);
        List<FlatFieldHistory> rows = buildGeneralHistory(snapshots, users);
        assertEquals(4, rows.size());
        assertEquals("A", rows.get(0).getField().getFieldValue());
        assertNull(rows.get(0).getPriorField().getFieldValue());
        assertNull(rows.get(1).getField().getFieldValue());
        assertEquals("B", rows.get(1).getPriorField().getFieldValue());
        assertEquals("user3", rows.get(1).getPriorField().getModifiedUser());
        assertEquals("user2", rows.get(2).getUsername());
        assertEquals("A", rows.get(2).getPriorField().getFieldValue());
        assertEquals(1, snapshots.get(3000L).size());
    }

    @Test public void missingFieldTypeDoesNotEraseChangesOrBreakRendering() {
        Field before = field("A"), after = field("B");
        before.setFieldType(null);
        after.setFieldType(null);
        Map<Long, List<Field>> snapshots = new HashMap<>();
        snapshots.put(1000L, Arrays.asList(before));
        snapshots.put(2000L, Arrays.asList(after));
        List<FlatFieldHistory> rows = buildGeneralHistory(snapshots, Collections.emptyMap());
        assertEquals(2, rows.size());
        assertFalse(rows.get(0).isValueOfTypeSignature());
    }

    @Test public void newlyPopulatedFieldUsesTheSameElapsedTimeAsItsChangeGroup() {
        Map<Long, List<Field>> snapshots = new HashMap<>();
        snapshots.put(1000L, Arrays.asList(field("A")));
        Field added = field("RFG");
        added.setFieldName("Region");
        snapshots.put(61000L, Arrays.asList(field("B"), added));
        List<FlatFieldHistory> rows = buildGeneralHistory(snapshots, Collections.emptyMap());
        assertEquals(3, rows.size());
        assertEquals("1 minute", rows.get(0).getDisplayValueForTimeDiff());
        assertEquals("1 minute", rows.get(1).getDisplayValueForTimeDiff());
        assertNull("The new field still has no previous value", rows.get(1).getPriorField());
        assertEquals("N/A", rows.get(2).getDisplayValueForTimeDiff());
    }

    @Test public void elapsedTimeUsesThePreviousSnapshotEvenWhenItHasNoVisibleValues() {
        Map<Long, List<Field>> snapshots = new HashMap<>();
        snapshots.put(1000L, Collections.emptyList());
        snapshots.put(61000L, Arrays.asList(field("A")));
        List<FlatFieldHistory> rows = buildGeneralHistory(snapshots, Collections.emptyMap());
        assertEquals(1, rows.size());
        assertEquals("1 minute", rows.get(0).getDisplayValueForTimeDiff());
        assertNull(rows.get(0).getPriorField());
    }

    @Test public void ordinaryTextIsNotParsedAsSignature() {
        FlatFieldHistory row = new FlatFieldHistory("user", 1000L, new Date(1000), field("Acme"), 1);
        assertFalse(row.isValueOfTypeSignature());
    }

    @Test public void permissionsStillRemoveHiddenFieldsAndRestoreVisibleFieldTypes() throws Exception {
        WebKitOpenFormFieldHistoryConversationBean bean = new WebKitOpenFormFieldHistoryConversationBean();
        Form record = new Form(20L);
        record.setFormType("Organisation Contact");
        bean.setHistoryForm(record);
        WebKitAccessBean access = new WebKitAccessBean() {
            @Override public List<Field> getFieldsViewableForFormDef(String type) {
                return "Organisation Contact".equals(type)
                        ? Arrays.asList(field(null)) : Collections.emptyList();
            }
        };
        java.lang.reflect.Field accessField = bean.getClass().getDeclaredField("accessBean");
        accessField.setAccessible(true);
        accessField.set(bean, access);
        Field allowed = field("A"), denied = field("secret");
        allowed.setFieldType(null);
        denied.setFieldName("Hidden Field");
        List<Field> fields = new ArrayList<>(Arrays.asList(allowed, denied));
        java.lang.reflect.Method filter = bean.getClass().getDeclaredMethod("removeFieldsWhereUserHasNoAccess", List.class);
        filter.setAccessible(true);
        filter.invoke(bean, fields);
        assertEquals(1, fields.size());
        assertEquals(Field.Type.Text, fields.get(0).getTypeAsEnum());
        record.setFormType("No Access");
        filter.invoke(bean, fields);
        assertTrue(fields.isEmpty());
    }

    @Test public void titleFollowsSelectedRecordRatherThanParentForm() {
        WebKitOpenFormFieldHistoryConversationBean bean = new WebKitOpenFormFieldHistoryConversationBean();
        Form parent = new Form(10L);
        parent.setTitle("Organisation Onboard Request");
        bean.setHistoryForm(parent);
        assertEquals("Organisation Onboard Request (#10)", bean.getHistoryTitle());
        Form record = new Form(20L);
        record.setFormType("Organisation Contact");
        bean.setHistoryForm(record);
        assertEquals("Organisation Contact (#20)", bean.getHistoryTitle());
    }
}
