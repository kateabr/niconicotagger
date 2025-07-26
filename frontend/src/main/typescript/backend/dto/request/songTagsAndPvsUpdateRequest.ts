import { PvToDisable } from "@/backend/dto/lowerLevelStruct";

export interface SongTagsAndPvsMassUpdateRequest {
  subRequests: SongTagsAndPvsUpdateRequest[];
  clientType: string;
}

export interface SongTagsAndPvsUpdateRequest {
  songId: number;
  pvId: string | null;
  tags: number[];
  nndPvsToDisable: PvToDisable[];
}
