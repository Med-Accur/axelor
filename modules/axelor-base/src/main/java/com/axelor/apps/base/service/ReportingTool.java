/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.apps.base.service;

import com.axelor.app.internal.AppFilter;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Localization;
import com.axelor.apps.base.service.localization.LocaleService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import java.util.Locale;
import java.util.Optional;

public class ReportingTool {

  /** Finds locale from user company. Defaults to user locale. */
  public static Locale getCompanyLocale() {
    // manage NPE using optional
    return Optional.ofNullable(AuthUtils.getUser())
        .map(User::getActiveCompany)
        .map(Company::getLocalization)
        .map(Localization::getCode)
        .map(LocaleService::computeLocaleByLocaleCode)
        .orElseGet(AppFilter::getLocale);
  }
}
