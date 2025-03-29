import { axios } from "@/backend/axios";
import { QueryConsoleResponse } from "@/backend/dto/response/queryConsoleResponse";
import { SongsWithPvsResponse } from "@/backend/dto/response/songsWithPvsResponse";
import { ReleaseEventResponse } from "@/backend/dto/response/releaseEventResponse";
import { ReleaseEventWithLinkedTagResponse } from "@/backend/dto/response/releaseEventWithLinkedTagResponse";
import { VideosByTagsResponseForEvent } from "@/backend/dto/response/videosByTagsResponseForEvent";
import { SongsByVocaDbEventTagResponse } from "@/backend/dto/response/songsByVocaDbEventTagResponse";
import { UpdateErrorReport } from "@/backend/dto/lowerLevelStruct";
import { VideosByTagsResponseForTagging } from "@/backend/dto/response/videosByTagsResponseForTagging";
import { VideosByVocaDbTagResponse } from "@/backend/dto/response/videosByVocaDbTagResponse";
import { MassAddReleaseEventRequest } from "@/backend/dto/request/addReleaseEventRequest";
import { MassDeleteTagUsagesRequest } from "@/backend/dto/request/deleteTagUsagesRequest";
import { GetReleaseEventRequest } from "@/backend/dto/request/getReleaseEventRequest";
import { LoginRequest } from "@/backend/dto/request/loginRequest";
import { QueryConsoleRequest } from "@/backend/dto/request/queryConsoleRequest";
import { SongsWithPvsRequest } from "@/backend/dto/request/songsWithPvsRequest";
import { VideosByNndTagsRequest } from "@/backend/dto/request/videosByNndTagsRequest";
import { VideosByNndEventTagsRequest } from "@/backend/dto/request/videosByNndEventTagsRequest";
import { VideosByVocaDbTagRequest } from "@/backend/dto/request/videosByVocaDbTagRequest";
import { SongTagsAndPvsMassUpdateRequest } from "@/backend/dto/request/songTagsAndPvsUpdateRequest";
import { SongsByVocaDbEventTagRequest } from "@/backend/dto/request/songsByVocaDbEventTagRequest";
import { SongTagsAndEventsMassUpdateRequest } from "@/backend/dto/request/songTagsAndEventsUpdateRequest";

export const api = {
  async authorize(payload: LoginRequest): Promise<void> {
    return axios.post("/api/authorize", payload);
  },
  async getVideosByNndTagsForTagging(
    payload: VideosByNndTagsRequest
  ): Promise<VideosByTagsResponseForTagging> {
    return axios.post("/api/get/videos/by_nnd_tags/for_tagging", payload).then(value => value.data);
  },
  async getVocaDbSongEntriesForTagging(
    payload: SongsWithPvsRequest
  ): Promise<SongsWithPvsResponse> {
    return axios.post("/api/get/songs", payload).then(value => value.data);
  },
  async getVideosByVocaDbTags(
    payload: VideosByVocaDbTagRequest
  ): Promise<VideosByVocaDbTagResponse> {
    return axios.post("/api/get/videos/by_vocadb_tag", payload).then(value => value.data);
  },
  async getReleaseEvent(payload: GetReleaseEventRequest): Promise<ReleaseEventResponse> {
    return axios.post("/api/get/release_event", payload).then(value => value.data);
  },
  async getReleaseEventWithLinkedTag(
    payload: GetReleaseEventRequest
  ): Promise<ReleaseEventWithLinkedTagResponse> {
    return axios.post("/api/get/release_event/with_linked_tags", payload).then(value => value.data);
  },
  async getVideosByNndTagsForEvent(
    payload: VideosByNndEventTagsRequest
  ): Promise<VideosByTagsResponseForEvent> {
    return axios.post("/api/get/videos/by_nnd_tags/for_event", payload).then(value => value.data);
  },
  async getVocaDbSongEntriesByVocaDbTagId(
    payload: SongsByVocaDbEventTagRequest
  ): Promise<SongsByVocaDbEventTagResponse> {
    return axios.post("/api/get/songs/by_vocadb_tag", payload).then(value => value.data);
  },
  async getDataByCustomQuery(payload: QueryConsoleRequest): Promise<QueryConsoleResponse> {
    return axios.post("/api/get/data/by_custom_query", payload).then(value => value.data);
  },
  async updateSongEventsAndTags(
    payload: SongTagsAndEventsMassUpdateRequest
  ): Promise<UpdateErrorReport[]> {
    return axios
      .post("/api/update/songs/replace_tag_with_event", payload)
      .then(value => value.data);
  },
  async updateSongTagsAndPvs(
    payload: SongTagsAndPvsMassUpdateRequest
  ): Promise<UpdateErrorReport[]> {
    return axios.post("/api/update/songs/update_tags_and_pvs", payload).then(value => value.data);
  },
  async addReleaseEvent(payload: MassAddReleaseEventRequest): Promise<UpdateErrorReport[]> {
    return axios.post("/api/update/songs/add_release_event", payload).then(value => value.data);
  },
  async removeTagUsages(payload: MassDeleteTagUsagesRequest): Promise<UpdateErrorReport[]> {
    return axios.post("/api/update/tags/delete", payload).then(value => value.data);
  }
};
