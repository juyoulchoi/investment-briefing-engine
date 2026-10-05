export type ExchangeRate = {
  base_date: string;
  base_currency: string;
  quote_currency: string;
  exchange_rate: number;
  previous_exchange_rate: number | null;
  change_amount: number | null;
  change_rate: number | null;
  high_52week_rate: number | null;
  low_52week_rate: number | null;
  data_source_code: string;
  data_status: string;
  collected_at: string;
};
