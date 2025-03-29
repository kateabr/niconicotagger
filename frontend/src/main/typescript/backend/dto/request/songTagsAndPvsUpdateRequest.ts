import { PvToDisable } from "@/backend/dto/lowerLevelStruct";
import { ClientType } from "@/backend/dto/enumeration";

export interface SongTagsAndPvsMassUpdateRequest {
  subRequests: SongTagsAndPvsUpdateRequest[];
  clientType: ClientType;
}

export interface SongTagsAndPvsUpdateRequest {
  songId: number;
  tags: number[];
  nndPvsToDisable: PvToDisable[];
}
