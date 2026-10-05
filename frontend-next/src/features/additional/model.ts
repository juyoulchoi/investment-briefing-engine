import type { Account } from "../../shared/types";
import type { Result } from "./types";
export function selectAdditionalAccount(
  data: Result,
  accountType: Account["accountType"],
) {
  const rows = data.candidates.filter((r) => r.accountType === accountType),
    accountSummary = data.accounts.find(
      (account) => account.accountType === accountType,
    ) || {
      accountId: 0,
      accountType,
      reserveAmount: 0,
      recommendedTotal: 0,
      usageRate: 0,
    };
  return { rows, accountSummary };
}
