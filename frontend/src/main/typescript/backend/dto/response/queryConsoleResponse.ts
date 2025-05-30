import { UpdateErrorReport, VocaDbTag } from "@/backend/dto/lowerLevelStruct";
import { ArtistType, SongType } from "@/backend/dto/enumeration";

export interface QueryConsoleResponse {
  items: (QueryConsoleArtistItem | QueryConsoleSongItem)[];
  totalCount: number;
  tagPool: VocaDbTag[];
}

export interface QueryConsoleArtistItem {
  id: number;
  name: string;
  tags: VocaDbTag[];
  type: ArtistType;
  tagsToRemove: VocaDbTag[];
  errorReport: UpdateErrorReport | null;
}

export interface QueryConsoleSongItem {
  id: number;
  name: string;
  tags: VocaDbTag[];
  type: string;
  artistString: string;
  tagsToRemove: VocaDbTag[];
  errorReport: UpdateErrorReport | null;
}
