"use client";
import { orderedAccountTypes } from "../../shared/account-context";
import { Summary, Table } from "../../shared/ui";

import { useHoldings } from "./useHoldings";
export default function HoldingsView() {
  const {
    accountTypes,
    accountLabel,
    selected,
    sort,
    setSort,
    loading,
    error,
    editMode,
    drafts,
    saving,
    saveError,
    adminAccounts,
    adminStocks,
    createOpen,
    setCreateOpen,
    createSaving,
    createError,
    newHolding,
    setNewHolding,
    isOverseas,
    isDomestic,
    isWholeWonAccount,
    holdingEvaluation,
    formatMoney,
    formatHoldingQuantity,
    rows,
    evaluation,
    profit,
    isBuyLocked,
    rowClass,
    beginEdit,
    cancelEdit,
    selectAccount,
    updateDraft,
    changed,
    openCreate,
    saveCreate,
    saveAccount,
  } = useHoldings();
  return (
    <div className="page">
      <div className="holding-toolbar">
        <div className="tabs account-tabs">
          {orderedAccountTypes(accountTypes).map((type) => (
            <button
              key={type}
              className={selected === type ? "active" : ""}
              onClick={() => selectAccount(type)}
              disabled={saving}
            >
              {accountLabel(type)}
            </button>
          ))}
        </div>
        <div className="holding-actions">
          <span className="latest">단위: {isOverseas ? "USD" : "원"}</span>
          <label className="sort-select">
            <span>정렬</span>
            <select
              value={sort}
              onChange={(e) => setSort(e.target.value)}
              disabled={editMode}
            >
              <option value="profitAsc">수익률 낮은순</option>
              <option value="profitDesc">수익률 높은순</option>
              <option value="evaluationDesc">평가금액 많은순</option>
            </select>
          </label>
          {editMode ? (
            <>
              <button
                className="cancel-button"
                onClick={cancelEdit}
                disabled={saving}
              >
                취소
              </button>
              <button
                className="primary"
                onClick={saveAccount}
                disabled={saving}
              >
                {saving ? "저장 중..." : `변경 ${changed.length}건 저장`}
              </button>
            </>
          ) : (
            <>
              <button
                className="primary"
                onClick={openCreate}
                disabled={loading || adminAccounts.length === 0}
              >
                + 종목 등록
              </button>
              <button
                className="primary"
                onClick={beginEdit}
                disabled={loading || rows.length === 0}
              >
                수정
              </button>
            </>
          )}
        </div>
      </div>
      {saveError && <div className="batch-error">{saveError}</div>}
      <Summary evaluation={evaluation} profit={profit} format={formatMoney} />
      <section className="card table holdings-table">
        {error && <div className="data-state error">{error}</div>}
        <Table
          heads={[
            "종목 코드",
            "종목명",
            "보유수량",
            ...(isDomestic ? ["정수주 매입금액", "소수점주 매입금액"] : []),
            "평균단가",
            "현재가",
            "평가금액",
            "손익률",
            "목표비중",
            "계좌 전체 비중",
            "투자자산 내 비중",
            "비중상태",
          ]}
        >
          {loading ? (
            <tr>
              <td colSpan={isDomestic ? 13 : 11} className="data-state">
                보유종목을 불러오는 중입니다.
              </td>
            </tr>
          ) : rows.length === 0 && !error ? (
            <tr>
              <td colSpan={isDomestic ? 13 : 11} className="data-state">
                등록된 보유종목이 없습니다.
              </td>
            </tr>
          ) : (
            rows.map((h) => (
              <tr key={h.holdingId} className={rowClass(h)}>
                <td>{h.stockCode}</td>
                <td>
                  <strong>{h.stockName}</strong>
                </td>
                <td>
                  {editMode ? (
                    <input
                      className="inline-edit"
                      type="number"
                      min="0"
                      step="0.00000001"
                      value={drafts[h.holdingId]?.holdingQuantity ?? ""}
                      disabled={isBuyLocked(h)}
                      title={
                        isBuyLocked(h)
                          ? "투자 설정에서 매수가 정지되어 수정할 수 없습니다."
                          : undefined
                      }
                      onChange={(e) =>
                        updateDraft(
                          h.holdingId,
                          "holdingQuantity",
                          e.target.value,
                        )
                      }
                    />
                  ) : (
                    formatHoldingQuantity(Number(h.holdingQuantity))
                  )}
                </td>
                {isDomestic && (
                  <td>
                    {editMode ? (
                      <input
                        className="inline-edit price"
                        type="number"
                        min="0"
                        step="0.0001"
                        value={
                          drafts[h.holdingId]?.wholeSharePurchaseAmount ?? ""
                        }
                        disabled={isBuyLocked(h)}
                        onChange={(e) =>
                          updateDraft(
                            h.holdingId,
                            "wholeSharePurchaseAmount",
                            e.target.value,
                          )
                        }
                      />
                    ) : (
                      formatMoney(Number(h.wholeSharePurchaseAmount ?? 0))
                    )}
                  </td>
                )}
                {isDomestic && (
                  <td>
                    {editMode ? (
                      <input
                        className="inline-edit price"
                        type="number"
                        min="0"
                        step="0.0001"
                        value={
                          drafts[h.holdingId]?.fractionalSharePurchaseAmount ??
                          ""
                        }
                        disabled={isBuyLocked(h)}
                        onChange={(e) =>
                          updateDraft(
                            h.holdingId,
                            "fractionalSharePurchaseAmount",
                            e.target.value,
                          )
                        }
                      />
                    ) : (
                      formatMoney(Number(h.fractionalSharePurchaseAmount ?? 0))
                    )}
                  </td>
                )}
                <td>
                  {editMode && !isDomestic ? (
                    <input
                      className="inline-edit price"
                      type="number"
                      min="0"
                      step={isWholeWonAccount ? "1" : "0.000001"}
                      value={drafts[h.holdingId]?.averagePrice ?? ""}
                      disabled={isBuyLocked(h)}
                      title={
                        isBuyLocked(h)
                          ? "투자 설정에서 매수가 정지되어 수정할 수 없습니다."
                          : undefined
                      }
                      onChange={(e) =>
                        updateDraft(h.holdingId, "averagePrice", e.target.value)
                      }
                    />
                  ) : editMode && isDomestic ? (
                    formatMoney(
                      Number(drafts[h.holdingId]?.holdingQuantity) > 0
                        ? (Number(
                            drafts[h.holdingId]?.wholeSharePurchaseAmount,
                          ) +
                            Number(
                              drafts[h.holdingId]
                                ?.fractionalSharePurchaseAmount,
                            )) /
                            Number(drafts[h.holdingId]?.holdingQuantity)
                        : 0,
                    )
                  ) : (
                    formatMoney(Number(h.averagePrice))
                  )}
                </td>
                <td>{formatMoney(Number(h.currentPrice))}</td>
                <td>
                  <strong>{formatMoney(holdingEvaluation(h))}</strong>
                </td>
                <td className={Number(h.profitLossRate) >= 0 ? "pos" : "neg"}>
                  {Number(h.profitLossRate) >= 0 ? "+" : ""}
                  {Number(h.profitLossRate).toFixed(2)}%
                </td>
                <td>
                  {h.targetWeight == null
                    ? "-"
                    : `${Number(h.targetWeight).toFixed(2)}%`}
                </td>
                <td>
                  {h.currentWeight == null
                    ? "-"
                    : `${Number(h.currentWeight).toFixed(2)}%`}
                </td>
                <td>
                  {h.investmentAssetWeight == null
                    ? "-"
                    : `${Number(h.investmentAssetWeight).toFixed(2)}%`}
                </td>
                <td title={h.weightStatus ?? undefined}>
                  {h.weightStatusName ?? "-"}
                </td>
              </tr>
            ))
          )}
        </Table>
      </section>
      {createOpen && (
        <div
          className="modal-backdrop"
          onMouseDown={(e) =>
            e.target === e.currentTarget && setCreateOpen(false)
          }
        >
          <section className="edit-modal ref-modal">
            <header>
              <div>
                <h3>보유종목 등록</h3>
                <p>{accountLabel(selected)} 계좌에 새 종목을 등록합니다.</p>
              </div>
              <button
                className="modal-close"
                onClick={() => setCreateOpen(false)}
              >
                ×
              </button>
            </header>
            <div className="ref-form">
              <label>
                <span>계좌 *</span>
                <select
                  value={newHolding.accountId ?? ""}
                  onChange={(e) =>
                    setNewHolding({
                      ...newHolding,
                      accountId: Number(e.target.value),
                    })
                  }
                >
                  {adminAccounts
                    .filter((a) => a.accountType === selected)
                    .map((a) => (
                      <option key={a.accountId} value={a.accountId}>
                        {accountLabel(a.accountType)}
                      </option>
                    ))}
                </select>
              </label>
              <label>
                <span>종목 *</span>
                <select
                  value={newHolding.stockId ?? ""}
                  onChange={(e) =>
                    setNewHolding({
                      ...newHolding,
                      stockId: Number(e.target.value),
                    })
                  }
                >
                  <option value="">종목 선택</option>
                  {adminStocks
                    .filter((st) => st.useYn === "Y")
                    .map((st) => (
                      <option key={st.stockId} value={st.stockId}>
                        {st.stockName} ({st.stockCode})
                      </option>
                    ))}
                </select>
              </label>
              <label>
                <span>보유수량 *</span>
                <input
                  type="number"
                  min="0"
                  step="0.00000001"
                  value={newHolding.holdingQuantity}
                  onChange={(e) =>
                    setNewHolding({
                      ...newHolding,
                      holdingQuantity: Number(e.target.value),
                    })
                  }
                />
              </label>
              <label>
                <span>평균 매입가 *</span>
                <input
                  type="number"
                  min="0"
                  step="any"
                  value={
                    isDomestic
                      ? Number(newHolding.holdingQuantity) > 0
                        ? (Number(newHolding.wholeSharePurchaseAmount) +
                            Number(newHolding.fractionalSharePurchaseAmount)) /
                          Number(newHolding.holdingQuantity)
                        : 0
                      : newHolding.averagePrice
                  }
                  disabled={isDomestic}
                  onChange={(e) =>
                    setNewHolding({
                      ...newHolding,
                      averagePrice: Number(e.target.value),
                    })
                  }
                />
              </label>
              {isDomestic && (
                <label>
                  <span>정수주 매입금액 *</span>
                  <input
                    type="number"
                    min="0"
                    step="0.0001"
                    value={newHolding.wholeSharePurchaseAmount}
                    onChange={(e) =>
                      setNewHolding({
                        ...newHolding,
                        wholeSharePurchaseAmount: Number(e.target.value),
                      })
                    }
                  />
                </label>
              )}
              {isDomestic && (
                <label>
                  <span>소수점주 매입금액 *</span>
                  <input
                    type="number"
                    min="0"
                    step="0.0001"
                    value={newHolding.fractionalSharePurchaseAmount}
                    onChange={(e) =>
                      setNewHolding({
                        ...newHolding,
                        fractionalSharePurchaseAmount: Number(e.target.value),
                      })
                    }
                  />
                </label>
              )}
              <label>
                <span>적용 환율 *</span>
                <input
                  type="number"
                  min="0"
                  step="any"
                  value={newHolding.exchangeRate}
                  onChange={(e) =>
                    setNewHolding({
                      ...newHolding,
                      exchangeRate: Number(e.target.value),
                    })
                  }
                />
              </label>
              <label>
                <span>목표 비중 (%)</span>
                <input
                  type="number"
                  min="0"
                  max="100"
                  step="any"
                  value={newHolding.targetWeight ?? ""}
                  onChange={(e) =>
                    setNewHolding({
                      ...newHolding,
                      targetWeight:
                        e.target.value === "" ? null : Number(e.target.value),
                    })
                  }
                />
              </label>
              <label className="wide">
                <span>메모</span>
                <input
                  value={newHolding.memo}
                  onChange={(e) =>
                    setNewHolding({ ...newHolding, memo: e.target.value })
                  }
                />
              </label>
            </div>
            {createError && <p className="form-error">{createError}</p>}
            <footer>
              <button
                className="cancel-button"
                onClick={() => setCreateOpen(false)}
              >
                취소
              </button>
              <button
                className="primary"
                disabled={createSaving}
                onClick={saveCreate}
              >
                {createSaving ? "저장 중..." : "등록"}
              </button>
            </footer>
          </section>
        </div>
      )}
    </div>
  );
}
