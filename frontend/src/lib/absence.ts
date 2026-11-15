import type { AbsenceType } from '../api/types';

export const ABSENCE_TYPES: { type: AbsenceType; label: string; hint?: string }[] = [
  { type: 'VACATION', label: 'Vacation' },
  { type: 'SICK', label: 'Sick leave' },
  { type: 'FLEX_DAY', label: 'Flextime day', hint: 'Reduces your overtime balance by the daily target.' },
  { type: 'PARENTAL_LEAVE', label: 'Parental leave' },
  { type: 'BUSINESS_TRIP', label: 'Business trip', hint: 'The daily target counts as worked time.' },
  { type: 'SPECIAL_LEAVE', label: 'Special leave', hint: 'E.g. wedding or moving. Does not use vacation days.' },
];

export function absenceLabel(type: AbsenceType): string {
  return ABSENCE_TYPES.find((t) => t.type === type)?.label ?? type;
}

/** CSS modifier for an absence or holiday type, e.g. FLEX_DAY becomes "flex-day". */
export function absenceCssKey(type: string | null): string {
  return (type ?? 'holiday').toLowerCase().replace(/_/g, '-');
}
