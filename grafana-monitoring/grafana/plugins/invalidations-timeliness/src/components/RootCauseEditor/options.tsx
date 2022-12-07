import { SELECTOPTIONS } from 'components/InvalidationEditor/options';
import { SelectCategory } from 'components/InvalidationEditor/types';
import _ from 'lodash';
import { PanelOptions } from 'types';
import { RootCauseEditorOptions } from './types';

export const DEFAULTVALUEROOTCAUSE: RootCauseEditorOptions[] = [];
export const getSelectOptions = (options: PanelOptions): SelectCategory => {
  const selectOptions = _.cloneDeep(SELECTOPTIONS);
  if (
    // impact === 'TIMELINESS' &&
    options?.rootCauseListTimeliness !== undefined &&
    options['rootCauseListTimeliness'].length > 0
  ) {
    selectOptions["TIMELINESS"]['rootCause']['options'] = options['rootCauseListTimeliness'];
    selectOptions["TIMELINESS"]['rootCause']['value'] = options['rootCauseListTimeliness'][0].value;
    // }
    // else if (
    //   impact === 'COMPLETENESS' &&
    //   options?.rootCauseListCompleteness !== undefined &&
    //   options['rootCauseListCompleteness'].length > 0
    // ) {
    //   selectOptions[impact]['rootCause']['options'] = options['rootCauseListCompleteness'];
    //   selectOptions[impact]['rootCause']['value'] = options['rootCauseListCompleteness'][0].value;
  } else {
    selectOptions["TIMELINESS"]['rootCause']['options'] = [];
    selectOptions["TIMELINESS"]['rootCause']['value'] = ' ';
  }
  return selectOptions["TIMELINESS"];
};
export const SELECTOPTIONSICONS = [
  {
    label: 'Check',
    imgUrl: 'public/plugins/cs-group-invalidations-timeliness/img/check.svg',
    value: 'public/plugins/cs-group-invalidations-timeliness/img/check.svg',
  },
  {
    label: 'Logo',
    imgUrl: 'public/plugins/cs-group-invalidations-timeliness/img/logo.svg',
    value: 'public/plugins/cs-group-invalidations-timeliness/img/logo.svg',
  },
  {
    label: 'On-Time',
    imgUrl: 'public/plugins/cs-group-invalidations-timeliness/img/on-time.svg',
    value: 'public/plugins/cs-group-invalidations-timeliness/img/on-time.svg',
  },
  {
    label: 'Orbit',
    imgUrl: 'public/plugins/cs-group-invalidations-timeliness/img/orbit.svg',
    value: 'public/plugins/cs-group-invalidations-timeliness/img/orbit.svg',
  },
  {
    label: 'Parabolic-Dishes',
    imgUrl: 'public/plugins/cs-group-invalidations-timeliness/img/parabolic-dishes.svg',
    value: 'public/plugins/cs-group-invalidations-timeliness/img/parabolic-dishes.svg',
  },
  {
    label: 'Question',
    imgUrl: 'public/plugins/cs-group-invalidations-timeliness/img/question.svg',
    value: 'public/plugins/cs-group-invalidations-timeliness/img/question.svg',
  },
  {
    label: 'Satellite',
    imgUrl: 'public/plugins/cs-group-invalidations-timeliness/img/satellite.svg',
    value: 'public/plugins/cs-group-invalidations-timeliness/img/satellite.svg',
  },
  {
    label: 'Settings',
    imgUrl: 'public/plugins/cs-group-invalidations-timeliness/img/settings.svg',
    value: 'public/plugins/cs-group-invalidations-timeliness/img/settings.svg',
  },
];
