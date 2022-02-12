import { axios } from "@/backend/axios";
import {
  AuthenticationPayload,
  AccessToken,
  FetchVideosPayload,
  VideosWithEntries,
  AssignTagPayload,
  FetchVideosFromDbPayload,
  EntriesWithVideos,
  LookupAndAssignTagPayload
} from "@/backend/dto";
import { AxiosResponse } from "axios";

export const api = {
  async authenticate(payload: AuthenticationPayload): Promise<AxiosResponse<AccessToken>> {
    return axios.post("/api/login", payload);
  },
  async fetchVideos(payload: FetchVideosPayload): Promise<VideosWithEntries> {
    return axios.post("/api/fetch", payload).then(value => value.data);
  },
  async fetchVideosFromDb(payload: FetchVideosFromDbPayload): Promise<EntriesWithVideos> {
    return axios.post("/api/fetch_from_db", payload).then(value => value.data);
  },
  async assignTag(payload: AssignTagPayload): Promise<number> {
    return axios.post("/api/assign", payload).then(value => value.data);
  },
  async lookUpAndAssignTag(payload: LookupAndAssignTagPayload): Promise<number> {
    return axios.post("/api/lookup_and_assign", payload).then(value => value.data);
  }
};
