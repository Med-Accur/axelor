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
package com.axelor.apps.account.service.move.attributes;

import com.axelor.apps.account.db.Journal;
import com.axelor.apps.account.db.Move;
import com.axelor.apps.base.AxelorException;
import com.axelor.auth.db.User;
import java.time.LocalDate;
import java.util.Map;

public interface MoveAttrsService {

  void addHidden(Move move, Map<String, Map<String, Object>> attrsMap);

  Map<String, Map<String, Object>> addFunctionalOriginSelectDomain(Journal journal);

  void addMoveLineListViewerHidden(Move move, Map<String, Map<String, Object>> attrsMap);

  void addPartnerDomain(Move move, Map<String, Map<String, Object>> attrsMap);

  void addPaymentModeDomain(Move move, Map<String, Map<String, Object>> attrsMap);

  void addPartnerBankDetailsDomain(Move move, Map<String, Map<String, Object>> attrsMap);

  void addTradingNameDomain(Move move, Map<String, Map<String, Object>> attrsMap);

  void addJournalDomain(Move move, Map<String, Map<String, Object>> attrsMap);

  void addWizardDefault(LocalDate moveDate, Map<String, Map<String, Object>> attrsMap);

  void addDueDateHidden(Move move, Map<String, Map<String, Object>> attrsMap);

  void addDateChangeTrueValue(Map<String, Map<String, Object>> attrsMap);

  void addDateChangeFalseValue(
      Move move, boolean paymentConditionChange, Map<String, Map<String, Object>> attrsMap);

  void addPaymentConditionChangeChangeValue(
      boolean value, Map<String, Map<String, Object>> attrsMap);

  void addHeaderChangeValue(boolean value, Map<String, Map<String, Object>> attrsMap);

  void getPfpAttrs(Move move, User user, Map<String, Map<String, Object>> attrsMap)
      throws AxelorException;

  void addMassEntryHidden(Move move, Map<String, Map<String, Object>> attrsMap);

  void addMassEntryPaymentConditionRequired(Move move, Map<String, Map<String, Object>> attrsMap);

  void addMassEntryBtnHidden(Move move, Map<String, Map<String, Object>> attrsMap);

  void addPartnerRequired(Move move, Map<String, Map<String, Object>> attrsMap);

  void addMainPanelTabHiddenValue(Move move, Map<String, Map<String, Object>> attrsMap);

  void addThirdPartyPayerPartnerReadonly(Move move, Map<String, Map<String, Object>> attrsMap);

  void addCompanyDomain(Move move, Map<String, Map<String, Object>> attrsMap);

  void addCompanyBankDetailsDomain(Move move, Map<String, Map<String, Object>> attrsMap);
}
