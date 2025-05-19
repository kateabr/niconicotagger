import { EventStatus, ReleaseEventCategory } from "@/backend/dto/enumeration";

export interface ReleaseEventPreview {
  id: number;
  date: string | null;
  endDate: string | null;
  dateString: string | null;
  name: string;
  category: ReleaseEventCategory;
  status: EventStatus;
  pictureUrl: string | null;
  isOffline: boolean;
}
