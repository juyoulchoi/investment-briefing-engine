package com.nanum.investment.common.query;

import com.querydsl.core.types.dsl.*;

public final class QuerydslExpressions {
    private QuerydslExpressions() {
    }

    public static BooleanExpression eqString(StringPath path, String value) {
        return value == null || value.isBlank() ? null : path.eq(value);
    }

    public static BooleanExpression eqLong(NumberPath<Long> path, Long value) {
        return value == null ? null : path.eq(value);
    }

    public static BooleanExpression notDeleted(StringPath path) {
        return path.eq("N");
    }
}
