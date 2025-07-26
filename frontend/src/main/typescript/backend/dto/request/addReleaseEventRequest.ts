import { ReleaseEvent } from "@/backend/dto/lowerLevelStruct";

export interface AddReleaseEventRequest {
  entryId: number;
  event: ReleaseEvent;
}

export interface MassAddReleaseEventRequest {
  subRequests: AddReleaseEventRequest[];
  clientType: string;
}
