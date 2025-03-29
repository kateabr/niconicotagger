import { ReleaseEventCategory } from "@/backend/dto/enumeration";

export interface ReleaseEventResponse {
  id: number;
  date: string | null;
  endDate: string | null;
  name: string;
  category: ReleaseEventCategory;
  nndTags: string[];
  suggestFiltering: boolean;
  seriesId: number | null;
}
