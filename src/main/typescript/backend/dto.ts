import { DateTime } from "luxon";
import { DateComparisonResult } from "@/utils";

export interface AuthenticationPayload {
  username: string;
  password: string;
  database: string;
}

export interface AccessToken {
  token: string;
}

export interface FetchVideosPayload {
  tag: string;
  scopeTag: string;
  startOffset: number;
  maxResults: number;
  orderBy: string;
}

export interface FetchVideosByEventTagsPayload {
  tags: string;
  scopeTag: string;
  startOffset: number;
  maxResults: number;
  orderBy: string;
  eventId: number;
  startTime: string | null;
  endTime: string | null;
}

export interface FetchVideosByEventTagPayload {
  tag: string;
  startOffset: number;
  maxResults: number;
  orderBy: string;
}

export interface FetchVideosFromDbPayload {
  startOffset: number;
  maxResults: number;
  orderBy: string;
}

export interface FetchVideosFromDbBeforeSincePayload {
  maxResults: number;
  mode: string;
  dateTime: string;
  songId: number;
  sortRule: string;
}

export interface NicoVideoWithTidyTags {
  contentId: string;
  title: string;
  startTime: string;
  tags: DisplayableTag[];
  eventDateComparison: DateComparisonResult | null;
}

export interface SongForApiContractSimplified {
  id: number;
  name: string;
  songType: string;
  artistString: string;
  createDate: string;
  tags: MinimalTag[];
}

export interface SongForApiContractSimplifiedWithReleaseEvent {
  id: number;
  name: string;
  songType: string;
  artistString: string;
  createDate: string;
  releaseEvent: ReleaseEventForApiContractSimplified | null;
  publishDate: string | null;
  eventDateComparison: DateComparisonResult;
  taggedWithMultipleEvents: boolean;
  taggedWithEventParticipant: boolean;
}

export interface VideoWithEntry {
  video: NicoVideoWithTidyTags;
  songEntry: SongForApiContractSimplifiedWithReleaseEvent | null;
  publisher: Publisher | null;
  processed: boolean;
}

export interface Publisher {
  entryTypeName: string;
  name: PublisherNames;
  id: number;
}

export interface PublisherNames {
  additionalNames: string;
  displayName: string;
}

export interface AssignableTag {
  additionalNames: string;
  categoryName: string;
  createDate: string;
  defaultNameLanguage: string;
  id: number;
  name: string;
  status: string;
  targets: number;
  urlSlug: string;
  usageCount: number;
  version: number;
}

export interface MinimalTag {
  id: number;
  name: string;
  urlSlug: string;
}

export interface VideosWithEntries {
  items: VideoWithEntry[];
  totalVideoCount: number;
  tags: AssignableTag[];
  tagMappings: string[];
  safeScope: string;
}

export interface VideosWithEntriesByVocaDbTag {
  items: VideoWithEntry[];
  totalVideoCount: number;
  tags: AssignableTag[];
  tagMappings: string[];
  safeScope: string;
}

export interface EntriesWithVideos {
  items: EntryWithVideos[];
  totalCount: number;
  timestampFirst: string;
  timestampLast: string;
}

export interface EntryWithVideos {
  thumbnailsOk: NicoVideoWithMappedTags[];
  thumbnailsErr: NicoVideoWithError[];
  song: SongForApiContractSimplified;
  totalVideoCount: number;
}

export interface AssignTagPayload {
  songId: number;
  tags: AssignableTag[];
}

export interface LookupAndAssignTagPayload {
  songId: number;
  tags: MinimalTag[];
  disable: string[];
}

export interface DisplayableTag {
  name: string;
  variant: string;
  locked: boolean;
}

export interface NicoVideoWithMappedTags {
  thumbnail: ThumbnailOk;
  mappedTags: MappedTag[];
  nicoTags: DisplayableTag[];
  expanded: boolean;
}

export interface ThumbnailOk {
  id: string;
  title: string;
  tags: DisplayableTag[];
}

export interface MappedTag {
  tag: MinimalTag;
  assigned: boolean;
  toAssign: boolean;
}

export interface NicoVideoWithError {
  id: string;
  code: string;
  description: string;
  title: string;
  disabled: boolean;
  community: boolean;
}

export interface ReleaseEventForApiContractSimplified {
  date: string | null;
  endDate: string | null;
  id: number;
  name: string;
  urlSlug: string;
  category: string;
  nndTags: string[];
}

export interface ReleaseEventForApiContractSimplifiedWithNndTags {
  event: ReleaseEventForApiContractSimplified;
  tags: string[];
}

export interface ReleaseEventForDisplay {
  date: DateTime | null;
  endDate: DateTime | null;
  id: number;
  name: string;
  urlSlug: string;
  category: string;
  nndTags: string[];
}

export interface EntriesWithReleaseEventTag {
  items: SongForApiContractSimplifiedWithReleaseEvent[];
  totalCount: number;
  releaseEvent: ReleaseEventForApiContractSimplified;
  eventTag: AssignableTag;
}

export interface EntryForTagRemoval {
  item: SongForApiContractSimplified;
  toRemove: boolean;
  tagIdsForRemoval: number[];
}

export interface EntriesForTagRemoval {
  items: EntryForTagRemoval[];
  totalCount: number;
  tagPool: AssignableTag[];
}

export interface AssignEventAndRemoveTagPayload {
  songId: number;
  event: MinimalEvent;
  tagId: number;
  participatedOnUpload: boolean;
}

export interface AssignEventPayload {
  songId: number;
  event: MinimalEvent;
  participatedOnUpload: boolean;
}

export interface FetchReleaseEventWithNndTagsPayload {
  eventName: string;
}

export interface CustomQueryPayload {
  query: string;
}

export interface TagsRemovalPayload {
  songId: number;
  tagIds: number[];
}

export interface MinimalEvent {
  id: number;
  name: string;
  urlSlug: string;
}
