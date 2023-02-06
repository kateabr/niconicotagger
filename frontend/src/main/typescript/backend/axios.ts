import originalAxios from "axios";
import { authenticationExpireHandler } from "@/backend/authenticationExpireHandler";

export const axios = originalAxios.create({
  timeout: 45 * 1000,
  baseURL: process.env.NODE_ENV !== "production" ? "" : "https://api.niconicotagger.handystuff.net"
});
axios.interceptors.request.use(
  value => {
    const accessToken = localStorage.getItem("accessToken");
    value.headers["Authorization"] = `Bearer ${accessToken}`;
    return value;
  },
  error => Promise.reject(error)
);
axios.interceptors.response.use(
  value => value,
  error => {
    const response = error.response;
    const status = error.status || (response ? response.status : 0);
    if (status === 401) authenticationExpireHandler();
    return Promise.reject(error);
  }
);
