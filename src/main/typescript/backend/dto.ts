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

export interface FetchVideosFromDbPayload {
  startOffset: number;
  maxResults: number;
  orderBy: string;
}

export interface NicoVideo {
  contentId: string;
  title: string;
  tags: string;
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
}

export interface VideoWithEntry {
  video: NicoVideoWithTidyTags;
  songEntry: SongForApiContractSimplified | null;
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
}

export interface VideosWithEntries {
  items: VideoWithEntry[];
  totalVideoCount: number;
  tags: AssignableTag[];
  tagMappings: string[];
  safeScope: string;
}

export interface EntriesWithVideos {
  items: EntryWithVideos[];
  totalCount: number;
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
}

export interface Ordering {
  name: string;
  value: string;
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
  contentId: string;
  code: string;
  description: string;
}
