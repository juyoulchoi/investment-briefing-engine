export const won = (n: number) => `${n.toLocaleString("ko-KR")}원`;

export const amount = (n: number, overseas = false) =>
  n.toLocaleString(overseas ? "en-US" : "ko-KR", {
    minimumFractionDigits: overseas ? 2 : 0,
    maximumFractionDigits: overseas ? 2 : 0,
  });

export const usd = (n: number) => `$${amount(n, true)}`;
