import {DateTime} from "luxon";

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
  tags: DisplayableTag[];
}

export interface SongForApiContractSimplified {
  id: number;
  name: string;
  tagInTags: boolean;
  songType: string;
  artistString: string;
  createDate: string;
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
}

export interface DateComparisonResult {
  dayDiff: number;
  disposition: "perfect" | "late" | "early" | "unknown";
}

export interface VideoWithEntry {
  video: NicoVideoWithTidyTags;
  songEntry: SongForApiContractSimplified | null;
  publisher: Publisher | null;
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
}

export interface ReleaseEventForDisplay {
  date: DateTime | null;
  endDate: DateTime | null;
  id: number;
  name: string;
  urlSlug: string;
  category: string;
}

export interface EntriesWithReleaseEventTag {
  items: SongForApiContractSimplifiedWithReleaseEvent[];
  totalCount: number;
  releaseEvent: ReleaseEventForApiContractSimplified;
  eventTag: AssignableTag;
}

export interface AssignEventAndRemoveTagPayload {
  songIds: number[];
  eventId: number;
  tagId: number;
}
``
