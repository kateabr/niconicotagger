import { ReleaseEvent, VocaDbTag } from "@/backend/dto/lowerLevelStruct";

export interface SongTagsAndEventsMassUpdateRequest {
  subRequests: SongTagsAndEventsUpdateRequest[];
  clientType: string;
}

export interface SongTagsAndEventsUpdateRequest {
  entryId: number;
  tags: VocaDbTag[];
  event: ReleaseEvent | null;
}
