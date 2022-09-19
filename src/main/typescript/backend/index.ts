import { axios } from "@/backend/axios";
import {
  AuthenticationPayload,
  AccessToken,
  FetchVideosPayload,
  VideosWithEntries,
  AssignTagPayload,
  FetchVideosFromDbPayload,
  EntriesWithVideos,
  LookupAndAssignTagPayload,
  FetchVideosFromDbBeforeSincePayload,
  VideosWithEntriesByVocaDbTag,
  FetchVideosByEventTagPayload,
  EntriesWithReleaseEventTag,
  AssignEventAndRemoveTagPayload,
  FetchReleaseEventWithNndTagsPayload,
  ReleaseEventForApiContractSimplifiedWithNndTags,
  FetchVideosByEventTagsPayload,
  AssignEventPayload,
  EntriesForTagRemoval,
  CustomQueryPayload,
  TagsRemovalPayload
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
  async fetchVideosFromDbBeforeSince(
    payload: FetchVideosFromDbBeforeSincePayload
  ): Promise<EntriesWithVideos> {
    return axios.post("/api/fetch_from_db_before_since", payload).then(value => value.data);
  },
  async assignTag(payload: AssignTagPayload): Promise<number> {
    return axios.post("/api/assign", payload).then(value => value.data);
  },
  async lookUpAndAssignTag(payload: LookupAndAssignTagPayload): Promise<number> {
    return axios.post("/api/lookup_and_assign", payload).then(value => value.data);
  },
  async fetchVideosByTag(payload: FetchVideosPayload): Promise<VideosWithEntriesByVocaDbTag> {
    return axios.post("/api/fetch_by_tag", payload).then(value => value.data);
  },
  async fetchEntriesFromDbByEventTag(
    payload: FetchVideosByEventTagPayload
  ): Promise<EntriesWithReleaseEventTag> {
    return axios.post("/api/fetch_from_db_by_event_tag", payload).then(value => value.data);
  },
  async assignEventAndRemoveTag(
    payload: AssignEventAndRemoveTagPayload
  ): Promise<EntriesWithReleaseEventTag> {
    return axios.post("/api/assign_event_and_remove_tag", payload).then(value => value.data);
  },
  async fetchReleaseEventWithNndTags(
    payload: FetchReleaseEventWithNndTagsPayload
  ): Promise<ReleaseEventForApiContractSimplifiedWithNndTags> {
    return axios.post("/api/fetch_release_event_with_nnd_tags", payload).then(value => value.data);
  },
  async fetchVideosByEventNndTags(
    payload: FetchVideosByEventTagsPayload
  ): Promise<VideosWithEntries> {
    return axios.post("/api/fetch_videos_by_event_nnd_tags", payload).then(value => value.data);
  },
  async assignEvent(payload: AssignEventPayload): Promise<EntriesWithReleaseEventTag> {
    return axios.post("/api/assign_event", payload).then(value => value.data);
  },
  async fetchSongsForTagRemoval(payload: CustomQueryPayload): Promise<EntriesForTagRemoval> {
    return axios.post("/api/fetch_songs_for_tag_removal", payload).then(value => value.data);
  },
  async removeTagsFromSong(payload: TagsRemovalPayload): Promise<EntriesForTagRemoval> {
    return axios.post("/api/remove_tags_from_song", payload).then(value => value.data);
  }
};
