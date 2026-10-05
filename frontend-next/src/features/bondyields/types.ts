export type BondYield = {
  base_date: string;
  bond_code: string;
  bond_name: string;
  country_code: string;
  maturity_months: number;
  yield_rate: number;
  previous_yield_rate: number | null;
  change_basis_points: number | null;
  data_source_code: string;
  data_status: string;
};
export type CommonCode = {
  code: string;
  name: string;
  description: string | null;
  displayOrder: number;
};
