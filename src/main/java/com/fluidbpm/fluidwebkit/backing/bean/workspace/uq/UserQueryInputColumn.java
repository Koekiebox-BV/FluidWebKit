package com.fluidbpm.fluidwebkit.backing.bean.workspace.uq;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean.ColumnModel;
import com.fluidbpm.program.api.vo.field.Field;
import lombok.Getter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Input metadata derived from a query rule, independently of its result columns. */
@Getter
public class UserQueryInputColumn extends ColumnModel {
    private static final Pattern RULE = Pattern.compile("^\\s*\\[([^]]+)\\]\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RANGE = Pattern.compile(
            "^BETWEEN\\s+(INPUT_VALUE|'[^']*')\\s+(?:AND|TO)\\s+(INPUT_VALUE|'[^']*')\\s*$",
            Pattern.CASE_INSENSITIVE);
    private final boolean range;
    private final boolean fromInput;
    private final boolean toInput;

    public UserQueryInputColumn(Field field, String rule) {
        super(field.getFieldName(), field.getFieldName(), field.getFieldDescription(),
                field.getTypeAsEnum(), field.getTypeMetaData(), true, true, true);
        Matcher match = rangeMatch(rule);
        this.range = match != null;
        this.fromInput = range && "INPUT_VALUE".equalsIgnoreCase(match.group(1));
        this.toInput = range && "INPUT_VALUE".equalsIgnoreCase(match.group(2));
    }

    public static String inputFieldName(String rule) {
        if (rule == null) return null;
        Matcher match = RULE.matcher(rule);
        if (!match.matches()) return null;
        Matcher range = rangeMatch(rule);
        boolean input = range == null ? match.group(2).trim().matches("(?i)(?:=|IN|LIKE)\\s+INPUT_VALUE") :
                "INPUT_VALUE".equalsIgnoreCase(range.group(1)) || "INPUT_VALUE".equalsIgnoreCase(range.group(2));
        return input ? match.group(1).trim() : null;
    }

    private static Matcher rangeMatch(String rule) {
        Matcher match = RULE.matcher(rule);
        if (!match.matches()) return null;
        Matcher range = RANGE.matcher(match.group(2));
        return range.matches() ? range : null;
    }
}
