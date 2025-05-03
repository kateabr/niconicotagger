export interface DispositionRelativelyToDate {
  disposition: DispositionType;
  byDays: number;
  color: DispositionColor;
}

export type DispositionColor = "error" | "warn" | "success";
export type DispositionType = "early" | "perfect" | "late";
