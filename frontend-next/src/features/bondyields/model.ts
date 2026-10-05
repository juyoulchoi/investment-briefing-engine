export const iso = (date: Date) => {
  const year = date.getFullYear(),
    month = String(date.getMonth() + 1).padStart(2, "0"),
    day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};
export const initialFrom = () => {
  const date = new Date();
  date.setDate(date.getDate() - 90);
  return iso(date);
};
