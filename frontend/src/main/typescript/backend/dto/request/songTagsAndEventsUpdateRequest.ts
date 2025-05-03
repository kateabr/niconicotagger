import { ReleaseEvent, VocaDbTag } from "@/backend/dto/lowerLevelStruct";
import { ClientType } from "@/backend/dto/enumeration";

export interface SongTagsAndEventsMassUpdateRequest {
  subRequsts: SongTagsAndEventsUpdateRequest[];
  clientType: ClientType;
}

export interface SongTagsAndEventsUpdateRequest {
  entryId: number;
  tags: VocaDbTag[];
  event: ReleaseEvent;
}
