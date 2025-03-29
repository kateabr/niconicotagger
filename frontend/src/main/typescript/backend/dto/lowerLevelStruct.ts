import { NndTagType, PublisherType } from "@/backend/dto/enumeration";

export interface EventDateBounds {
  from: string | null;
  to: string | null;
  applyToSearch: boolean;
}

export interface ReleaseEvent {
  id: number;
  name: string;
}

export interface ReleaseEventWithSeriesId {
  id: number;
  name: string;
  seriesId: number | null;
}

export interface VocaDbTag {
  id: number;
  name: string;
}

export interface PvToDisable {
  id: string;
  reason: string;
}

export interface NndTagData {
  name: string;
  type: NndTagType;
  locked: boolean;
}

export interface VocaDbTagSelectable {
  tag: VocaDbTag;
  selected: boolean;
}

export interface ErrorData {
  statusText: string;
  message: string;
}

export interface PublisherInfo {
  link: string;
  linkText: string | null;
  name: string | null;
  type: PublisherType;
}

export interface UpdateErrorReport {
  entryId: number;
  message: string | null;
}
