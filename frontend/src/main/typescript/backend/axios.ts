import originalAxios from "axios";
import { authenticationExpireHandler } from "@/backend/authenticationExpireHandler";

export const axios = originalAxios.create({
  timeout: 5 * 60 * 1000,
  withCredentials: true,
  baseURL: process.env.NODE_ENV !== "production" ? "" : "https://api.niconicotagger.handystuff.net"
});
axios.interceptors.response.use(
  value => value,
  error => {
    const response = error.response;
    const status = error.status || (response ? response.status : 0);
    if (status === 401) authenticationExpireHandler();
    return Promise.reject(error);
  }
);
