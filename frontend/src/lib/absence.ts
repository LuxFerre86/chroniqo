import type { AbsenceType } from '../api/types';
import { translate } from '../i18n';

export function getAbsenceTypes(): { type: AbsenceType; label: string; hint?: string }[] {
  return [
    { type: 'VACATION', label: translate('absence.vacation') },
    { type: 'SICK', label: translate('absence.sick') },
    { type: 'FLEX_DAY', label: translate('absence.flexDay'), hint: translate('absence.flexDay.hint') },
    { type: 'PARENTAL_LEAVE', label: translate('absence.parentalLeave') },
    { type: 'BUSINESS_TRIP', label: translate('absence.businessTrip'), hint: translate('absence.businessTrip.hint') },
    { type: 'SPECIAL_LEAVE', label: translate('absence.specialLeave'), hint: translate('absence.specialLeave.hint') },
  ];
}

export function absenceLabel(type: AbsenceType): string {
  return getAbsenceTypes().find((t) => t.type === type)?.label ?? type;
}

/** CSS modifier for an absence or holiday type, e.g. FLEX_DAY becomes "flex-day". */
export function absenceCssKey(type: string | null): string {
  return (type ?? 'holiday').toLowerCase().replace(/_/g, '-');
}