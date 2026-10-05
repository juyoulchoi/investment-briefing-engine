"use client";
import { createContext, useContext } from "react";
export const NotificationContext = createContext<(message: string) => void>(
  () => {},
);
export const useNotify = () => useContext(NotificationContext);
