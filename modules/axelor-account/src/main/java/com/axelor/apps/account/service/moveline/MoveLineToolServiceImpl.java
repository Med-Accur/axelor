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
package com.axelor.apps.account.service.moveline;

import com.axelor.apps.account.db.Account;
import com.axelor.apps.account.db.AccountType;
import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.Journal;
import com.axelor.apps.account.db.Move;
import com.axelor.apps.account.db.MoveLine;
import com.axelor.apps.account.db.Tax;
import com.axelor.apps.account.db.TaxLine;
import com.axelor.apps.account.db.repo.AccountTypeRepository;
import com.axelor.apps.account.db.repo.MoveLineRepository;
import com.axelor.apps.account.db.repo.MoveRepository;
import com.axelor.apps.account.exception.AccountExceptionMessage;
import com.axelor.apps.account.service.TaxAccountService;
import com.axelor.apps.account.service.move.MoveToolService;
import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Currency;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.repo.TraceBackRepository;
import com.axelor.apps.base.service.CurrencyService;
import com.axelor.apps.base.service.exception.TraceBackService;
import com.axelor.db.JPA;
import com.axelor.i18n.I18n;
import com.axelor.rpc.Context;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.google.inject.servlet.RequestScoped;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.persistence.Query;
import org.apache.commons.collections.CollectionUtils;

@RequestScoped
public class MoveLineToolServiceImpl implements MoveLineToolService {
  protected static final int RETURNED_SCALE = 2;

  protected TaxAccountService taxAccountService;
  protected CurrencyService currencyService;
  protected MoveLineRepository moveLineRepository;
  protected MoveToolService moveToolService;

  @Inject
  public MoveLineToolServiceImpl(
      TaxAccountService taxAccountService,
      CurrencyService currencyService,
      MoveLineRepository moveLineRepository,
      MoveToolService moveToolService) {
    this.taxAccountService = taxAccountService;
    this.currencyService = currencyService;
    this.moveLineRepository = moveLineRepository;
    this.moveToolService = moveToolService;
  }

  /**
   * Fonction permettant de récuperer la ligne d'écriture (au credit et non complétement lettrée sur
   * le compte client) de la facture
   *
   * @param invoice Une facture
   * @return
   */
  @Override
  public MoveLine getCreditCustomerMoveLine(Invoice invoice) {
    if (invoice.getMove() != null) {
      return this.getCreditCustomerMoveLine(invoice.getMove());
    }
    return null;
  }

  /**
   * Method that returns all credit move lines of an invoice that are not completely lettered
   *
   * @param invoice Invoice
   * @return
   */
  @Override
  public List<MoveLine> getCreditCustomerMoveLines(Invoice invoice) {
    List<MoveLine> moveLines = new ArrayList<MoveLine>();
    if (invoice.getMove() != null) {
      moveLines = getCreditCustomerMoveLines(invoice.getMove());
    }
    return moveLines;
  }

  /**
   * Method that returns all credit move lines of a move that are not completely lettered
   *
   * @param move Invoice move
   * @return
   */
  @Override
  public List<MoveLine> getCreditCustomerMoveLines(Move move) {

    List<MoveLine> moveLines = Lists.newArrayList();
    for (MoveLine moveLine : move.getMoveLineList()) {
      if (moveLine.getAccount().getUseForPartnerBalance()
          && moveLine.getCredit().compareTo(BigDecimal.ZERO) > 0
          && moveLine.getAmountRemaining().compareTo(BigDecimal.ZERO) != 0) {
        moveLines.add(moveLine);
      }
    }
    return moveLines;
  }

  /**
   * Fonction permettant de récuperer la ligne d'écriture (au credit et non complétement lettrée sur
   * le compte client) de l'écriture de facture
   *
   * @param move Une écriture de facture
   * @return
   */
  @Override
  public MoveLine getCreditCustomerMoveLine(Move move) {
    for (MoveLine moveLine : move.getMoveLineList()) {
      if (moveLine.getAccount().getUseForPartnerBalance()
          && moveLine.getCredit().compareTo(BigDecimal.ZERO) > 0
          && moveLine.getAmountRemaining().abs().compareTo(BigDecimal.ZERO) != 0) {
        return moveLine;
      }
    }
    return null;
  }

  /**
   * Fonction permettant de récuperer la ligne d'écriture (au débit et non complétement lettrée sur
   * le compte client) de la facture
   *
   * @param invoice Une facture
   * @return
   */
  @Override
  public MoveLine getDebitCustomerMoveLine(Invoice invoice) {
    if (invoice.getMove() != null) {
      return this.getDebitCustomerMoveLine(invoice.getMove());
    }
    return null;
  }

  /**
   * Method that returns all debit move lines of an invoice that are not completely lettered
   *
   * @param invoice Invoice
   * @return
   */
  @Override
  public List<MoveLine> getDebitCustomerMoveLines(Invoice invoice) {
    List<MoveLine> moveLines = new ArrayList<MoveLine>();
    if (invoice.getMove() != null) {
      moveLines = this.getDebitCustomerMoveLines(invoice.getMove());
    }
    return moveLines;
  }

  /**
   * Fonction permettant de récuperer la ligne d'écriture (au débit et non complétement lettrée sur
   * le compte client) de l'écriture de facture
   *
   * @param move Une écriture de facture
   * @return
   */
  @Override
  public MoveLine getDebitCustomerMoveLine(Move move) {
    for (MoveLine moveLine : move.getMoveLineList()) {
      if (moveLine.getAccount().getUseForPartnerBalance()
          && moveLine.getDebit().compareTo(BigDecimal.ZERO) > 0
          && moveLine.getAmountRemaining().abs().compareTo(BigDecimal.ZERO) > 0) {
        return moveLine;
      }
    }
    return null;
  }

  /**
   * Method that returns all debit move lines of a move that are not completely lettered
   *
   * @param move Invoice move
   * @return
   */
  @Override
  public List<MoveLine> getDebitCustomerMoveLines(Move move) {

    List<MoveLine> moveLines = Lists.newArrayList();
    for (MoveLine moveLine : move.getMoveLineList()) {
      if (moveLine.getAccount().getUseForPartnerBalance()
          && moveLine.getDebit().compareTo(BigDecimal.ZERO) > 0
          && moveLine.getAmountRemaining().compareTo(BigDecimal.ZERO) != 0) {
        moveLines.add(moveLine);
      }
    }
    return moveLines;
  }

  /**
   * Fonction permettant de générér automatiquement la description des lignes d'écritures
   *
   * @param journal Le journal de l'écriture
   * @param origin Le n° pièce réglée, facture, avoir ou de l'opération rejetée
   * @return
   */
  @Override
  public String determineDescriptionMoveLine(Journal journal, String origin, String description) {
    String descriptionComputed = "";
    if (journal == null) {
      return "";
    }

    if (journal.getDescriptionModel() != null) {
      descriptionComputed += journal.getDescriptionModel();
    }

    if (journal.getDescriptionIdentificationOk() && origin != null) {
      if (!descriptionComputed.isEmpty()) {
        descriptionComputed += " ";
      }
      descriptionComputed += origin;
    }

    if (!journal.getIsInvoiceMoveConsolidated() && description != null) {
      if (!descriptionComputed.isEmpty()) {
        descriptionComputed += " - ";
      }
      descriptionComputed += description;
    }
    return descriptionComputed;
  }

  /**
   * Method used to recover all credit reconciliable move line from a move line list
   *
   * @param moveLineList
   * @return reconciliableCreditMoveLineList
   */
  @Override
  public List<MoveLine> getReconciliableCreditMoveLines(List<MoveLine> moveLineList) {

    List<MoveLine> reconciliableCreditMoveLineList = new ArrayList<>();

    for (MoveLine moveLine : moveLineList) {
      if (moveLine.getAccount().getReconcileOk()
          && moveLine.getCredit().compareTo(BigDecimal.ZERO) > 0
          && moveLine.getDebit().compareTo(BigDecimal.ZERO) == 0) {
        reconciliableCreditMoveLineList.add(moveLine);
      }
    }

    return reconciliableCreditMoveLineList;
  }

  /**
   * Method used to recover all debit reconciliable move line from a move line list
   *
   * @param moveLineList
   * @return reconciliableDebitMoveLineList
   */
  @Override
  public List<MoveLine> getReconciliableDebitMoveLines(List<MoveLine> moveLineList) {

    List<MoveLine> reconciliableDebitMoveLineList = new ArrayList<>();

    for (MoveLine moveLine : moveLineList) {
      if (moveLine.getAccount().getReconcileOk()
          && moveLine.getCredit().compareTo(BigDecimal.ZERO) == 0
          && moveLine.getDebit().compareTo(BigDecimal.ZERO) > 0) {
        reconciliableDebitMoveLineList.add(moveLine);
      }
    }

    return reconciliableDebitMoveLineList;
  }

  @Override
  public MoveLine setCurrencyAmount(MoveLine moveLine) {
    Move move = moveLine.getMove();
    boolean isDebit = moveLine.getDebit().compareTo(moveLine.getCredit()) > 0;
    int returnedScale = RETURNED_SCALE;

    if (move.getMoveLineList().size() == 0 || moveLine.getCurrencyRate().signum() == 0) {
      try {
        moveLine.setCurrencyRate(
            currencyService.getCurrencyConversionRate(
                move.getCurrency(), move.getCompanyCurrency(), move.getDate()));
      } catch (AxelorException e1) {
        TraceBackService.trace(e1);
      }
    } else {
      moveLine.setCurrencyRate(move.getMoveLineList().get(0).getCurrencyRate());
    }

    if (move.getCurrency() != null) {
      returnedScale = move.getCurrency().getNumberOfDecimals();
    }

    BigDecimal unratedAmount = moveLine.getDebit().add(moveLine.getCredit());
    BigDecimal currencyAmount =
        unratedAmount.divide(moveLine.getCurrencyRate(), returnedScale, RoundingMode.HALF_UP);
    moveLine.setCurrencyAmount(this.computeCurrencyAmountSign(currencyAmount, isDebit));
    return moveLine;
  }

  @Override
  public boolean checkCutOffDates(MoveLine moveLine) {
    return moveLine == null
        || moveLine.getAccount() == null
        || !moveLine.getAccount().getManageCutOffPeriod()
        || this.isCutOffActive(moveLine);
  }

  @Override
  public boolean isEqualTaxMoveLine(
      Account account, Set<TaxLine> taxLineSet, Integer vatSystem, Long id, MoveLine ml) {
    return CollectionUtils.isNotEmpty(ml.getTaxLineSet())
        && ml.getTaxLineSet().equals(taxLineSet)
        && Objects.equals(ml.getVatSystemSelect(), vatSystem)
        && !Objects.equals(ml.getId(), id)
        && ml.getAccount().getAccountType() != null
        && this.isMoveLineTaxAccount(ml)
        && ml.getAccount().equals(account);
  }

  public void checkDateInPeriod(Move move, MoveLine moveLine) throws AxelorException {
    if (move != null
        && move.getPeriod() != null
        && moveLine != null
        && moveLine.getDate() != null
        && (moveLine.getDate().isBefore(move.getPeriod().getFromDate())
            || moveLine.getDate().isAfter(move.getPeriod().getToDate()))) {
      if (move.getCurrency() != null
          && move.getCurrency().getSymbol() != null
          && moveLine.getAccount() != null) {
        throw new AxelorException(
            moveLine,
            TraceBackRepository.CATEGORY_MISSING_FIELD,
            I18n.get(AccountExceptionMessage.DATE_NOT_IN_PERIOD_MOVE),
            moveLine.getCurrencyAmount().abs(),
            move.getCurrency().getSymbol(),
            moveLine.getAccount().getCode());
      } else {
        throw new AxelorException(
            moveLine,
            TraceBackRepository.CATEGORY_MISSING_FIELD,
            I18n.get(AccountExceptionMessage.DATE_NOT_IN_PERIOD_MOVE_WITHOUT_ACCOUNT));
      }
    }
  }

  @Override
  @Transactional
  public void setAmountRemainingReconciliableMoveLines(Context context) {
    Long accountId = Long.valueOf(context.get("_accountId").toString());
    String startDate = (String) context.get("startDate");

    Query update =
        JPA.em()
            .createQuery(
                "UPDATE MoveLine self SET self.amountRemaining = GREATEST(self.credit, self.debit) "
                    + "WHERE self.id IN (SELECT self.id FROM MoveLine self WHERE self.accountId = :accountId"
                    + (startDate == null ? ")" : " AND self.move.date >= :startDate)"));
    update.setParameter("accountId", accountId);

    if (startDate != null) {
      update.setParameter("startDate", LocalDate.parse(startDate));
    }
    update.executeUpdate();
  }

  @Override
  public boolean isCutOffActive(MoveLine moveLine) {
    List<Integer> functionalOriginList =
        Arrays.asList(
            MoveRepository.FUNCTIONAL_ORIGIN_OPENING,
            MoveRepository.FUNCTIONAL_ORIGIN_CLOSURE,
            MoveRepository.FUNCTIONAL_ORIGIN_CUT_OFF);

    return moveLine.getCutOffStartDate() != null
        && moveLine.getCutOffEndDate() != null
        && !functionalOriginList.contains(moveLine.getMove().getFunctionalOriginSelect());
  }

  @Override
  public void setDecimals(MoveLine moveLine, Move move) {
    Currency currency = move.getCurrency();
    Currency companyCurrency = move.getCompanyCurrency();

    if (currency != null && companyCurrency != null) {
      moveLine.setCurrencyDecimals(currency.getNumberOfDecimals());
      moveLine.setCompanyCurrencyDecimals(companyCurrency.getNumberOfDecimals());
    }
  }

  @Override
  public List<MoveLine> getMoveExcessDueList(
      boolean excessPayment, Company company, Partner partner, Long invoiceId) {
    String filter = "";
    if (excessPayment) {
      filter = "self.credit > 0";
    } else {
      filter = "self.debit > 0";
    }

    filter =
        filter.concat(
            " AND self.move.company = :company AND (self.move.statusSelect = :statusAccounted OR self.move.statusSelect = :statusDaybook) "
                + " AND self.move.ignoreInAccountingOk IN (false,null)"
                + " AND self.account.accountType.technicalTypeSelect not in (:technicalTypesToExclude)"
                + " AND self.account.useForPartnerBalance = true AND self.amountRemaining > 0 "
                + " AND self.partner = :partner AND (self.move.invoice IS NULL OR self.move.invoice.id != :invoiceId) ORDER BY self.date ASC ");

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("company", company);
    bindings.put("statusAccounted", MoveRepository.STATUS_ACCOUNTED);
    bindings.put("statusDaybook", MoveRepository.STATUS_DAYBOOK);
    bindings.put(
        "technicalTypesToExclude",
        Arrays.asList(AccountTypeRepository.TYPE_VIEW, AccountTypeRepository.TYPE_TAX));
    bindings.put("partner", partner);
    bindings.put("invoiceId", invoiceId);

    return moveLineRepository.all().filter(filter).bind(bindings).fetch();
  }

  @Override
  public boolean isMoveLineTaxAccount(MoveLine moveLine) {
    return AccountTypeRepository.TYPE_TAX.equals(
        Optional.of(moveLine)
            .map(MoveLine::getAccount)
            .map(Account::getAccountType)
            .map(AccountType::getTechnicalTypeSelect)
            .orElse(""));
  }

  @Override
  public boolean isMoveLineTaxAccountOrNonDeductibleTax(MoveLine moveLine) {
    return this.isMoveLineTaxAccount(moveLine)
        || (taxAccountService.isNonDeductibleTaxesSet(moveLine.getTaxLineSet())
            && moveLine.getIsNonDeductibleTax());
  }

  @Override
  public void setIsNonDeductibleTax(MoveLine moveLine, Tax tax) {
    if (tax.getIsNonDeductibleTax()) {
      moveLine.setIsNonDeductibleTax(true);
    }
  }

  @Override
  public BigDecimal computeCurrencyAmountSign(BigDecimal currencyAmount, boolean isDebit) {
    if (isDebit) {
      return currencyAmount.abs();
    } else {
      return currencyAmount.compareTo(BigDecimal.ZERO) < 0
          ? currencyAmount
          : currencyAmount.negate();
    }
  }

  @Override
  public boolean isMoveLineSpecialAccount(MoveLine moveLine) {
    return AccountTypeRepository.TYPE_SPECIAL.equals(
        Optional.of(moveLine)
            .map(MoveLine::getAccount)
            .map(Account::getAccountType)
            .map(AccountType::getTechnicalTypeSelect)
            .orElse(""));
  }

  @Override
  public boolean isMoveLineCommitmentAccount(MoveLine moveLine) {
    return AccountTypeRepository.TYPE_COMMITMENT.equals(
        Optional.of(moveLine)
            .map(MoveLine::getAccount)
            .map(Account::getAccountType)
            .map(AccountType::getTechnicalTypeSelect)
            .orElse(""));
  }
}
