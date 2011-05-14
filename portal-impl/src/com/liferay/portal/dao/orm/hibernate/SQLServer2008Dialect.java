/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.dao.orm.hibernate;

import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.dialect.SQLServerDialect;

/**
 * @author Steven Cao
 */
public class SQLServer2008Dialect extends SQLServerDialect {

	public boolean dropTemporaryTableAfterUse() {
		return _DROP_TEMP_TABLE_AFTER_USE;
	}

	public String getLimitString(String sql, int offset, int limit) {
		String sqlLowerCase = sql.toLowerCase();

		int fromPos = sqlLowerCase.indexOf(" from ");

		String selectFrom = sql.substring(0, fromPos);

		int orderByPos = sqlLowerCase.lastIndexOf(" order by ");

		String selectFromWhere = null;
		String orderBy = null;

		if (orderByPos > 0) {
			selectFromWhere = sql.substring(fromPos, orderByPos);
			orderBy = sql.substring(orderByPos + 9, sql.length());
		}
		else {
			selectFromWhere = sql.substring(fromPos);
			orderBy = "CURRENT_TIMESTAMP";
		}

		StringBundler sb = null;

		if (sqlLowerCase.contains(" union ")) {
			String[] orderByColumns = getOrderByColumns(
				selectFrom, orderBy, false);

			sb = new StringBundler(11);

			sb.append("select * from (select *, row_number() over (order by ");
			sb.append(StringUtil.merge(orderByColumns, StringPool.COMMA));
			sb.append(") as _page_row_num from (");
			sb.append(selectFrom);
			sb.append(selectFromWhere);
			sb.append(" ) _temp_table_1 ) _temp_table_2");
		}
		else {
			String[] orderByColumns = getOrderByColumns(
				selectFrom, orderBy, true);

			sb = new StringBundler(12);

			sb.append("select * from (");
			sb.append(selectFrom);
			sb.append(", row_number() over (order by ");
			sb.append(StringUtil.merge(orderByColumns, StringPool.COMMA));
			sb.append(") as _page_row_num ");
			sb.append(selectFromWhere);
			sb.append(" ) _temp_table_1");
		}

		sb.append(" where _page_row_num between ");
		sb.append(offset + 1);
		sb.append(" and ");
		sb.append(limit);
		sb.append(" order by _page_row_num");

		return sb.toString();
	}

	public boolean supportsLimitOffset() {
		return _SUPPORTS_LIMIT_OFFSET;
	}

	protected String[] getOrderByColumns(
		String selectFrom, String orderBy, boolean useOriginalColumnNames) {

		String[] orderByColumns = StringUtil.split(orderBy, StringPool.COMMA);

		for (int i = 0; i < orderByColumns.length; i++) {
			String orderByColumn = orderByColumns[i].trim();

			String orderByColumnName = null;
			String orderByType = null;

			int columnPos = orderByColumn.indexOf(CharPool.SPACE);

			if (columnPos == -1) {
				orderByColumnName = orderByColumn;
				orderByType = "ASC";
			}
			else {
				orderByColumnName = orderByColumn.substring(0, columnPos);
				orderByType = orderByColumn.substring(columnPos + 1);
			}

			if (useOriginalColumnNames) {
				String patternString = "(\\S+) as \\Q".concat(
					orderByColumnName).concat("\\E\\W");

				Pattern pattern = Pattern.compile(
					patternString, Pattern.CASE_INSENSITIVE);

				Matcher matcher = pattern.matcher(selectFrom);

				if (matcher.find()) {
					orderByColumnName = matcher.group(1);
				}
			}

			orderByColumns[i] = orderByColumnName.concat(
				StringPool.SPACE).concat(orderByType);
		}

		return orderByColumns;
	}

	private static final boolean _DROP_TEMP_TABLE_AFTER_USE = true;

	private static final boolean _SUPPORTS_LIMIT_OFFSET = true;

}