import { SELECTOPTIONS } from 'components/InvalidationEditor/options';
import { SelectCategory } from 'components/InvalidationEditor/types';
import _ from 'lodash';
import { PanelOptions } from 'types';
import { RootCauseEditorOptions } from './types';

export const DEFAULTVALUEROOTCAUSE: RootCauseEditorOptions[] = [];
export const getSelectOptions = (options: PanelOptions, impact: string): SelectCategory => {
  const selectOptions = _.cloneDeep(SELECTOPTIONS);
  if (
    impact === 'TIMELINESS' &&
    options?.rootCauseListTimeliness !== undefined &&
    options['rootCauseListTimeliness'].length > 0
  ) {
    selectOptions[impact]['rootCause']['options'] = options['rootCauseListTimeliness'];
    selectOptions[impact]['rootCause']['value'] = options['rootCauseListTimeliness'][0].value;
  } else if (
    impact === 'COMPLETENESS' &&
    options?.rootCauseListCompleteness !== undefined &&
    options['rootCauseListCompleteness'].length > 0
  ) {
    selectOptions[impact]['rootCause']['options'] = options['rootCauseListCompleteness'];
    selectOptions[impact]['rootCause']['value'] = options['rootCauseListCompleteness'][0].value;
  } else {
    selectOptions[impact]['rootCause']['options'] = [];
    selectOptions[impact]['rootCause']['value'] = ' ';
  }
  return selectOptions[impact];
};
export const SELECTOPTIONSICONS = [
  {
    label: 'Check',
    imgUrl: 'public/plugins/cs-group-invalidation-slice/img/check.svg',
    value: 'public/plugins/cs-group-invalidation-slice/img/check.svg',
  },
  {
    label: 'Logo',
    imgUrl: 'public/plugins/cs-group-invalidation-slice/img/logo.svg',
    value: 'public/plugins/cs-group-invalidation-slice/img/logo.svg',
  },
  {
    label: 'On-Time',
    imgUrl: 'public/plugins/cs-group-invalidation-slice/img/on-time.svg',
    value: 'public/plugins/cs-group-invalidation-slice/img/on-time.svg',
  },
  {
    label: 'Orbit',
    imgUrl: 'public/plugins/cs-group-invalidation-slice/img/orbit.svg',
    value: 'public/plugins/cs-group-invalidation-slice/img/orbit.svg',
  },
  {
    label: 'Parabolic-Dishes',
    imgUrl: 'public/plugins/cs-group-invalidation-slice/img/parabolic-dishes.svg',
    value: 'public/plugins/cs-group-invalidation-slice/img/parabolic-dishes.svg',
  },
  {
    label: 'Question',
    imgUrl: 'public/plugins/cs-group-invalidation-slice/img/question.svg',
    value: 'public/plugins/cs-group-invalidation-slice/img/question.svg',
  },
  {
    label: 'Satellite',
    imgUrl: 'public/plugins/cs-group-invalidation-slice/img/satellite.svg',
    value: 'public/plugins/cs-group-invalidation-slice/img/satellite.svg',
  },
  {
    label: 'Settings',
    imgUrl: 'public/plugins/cs-group-invalidation-slice/img/settings.svg',
    value: 'public/plugins/cs-group-invalidation-slice/img/settings.svg',
  },
];
