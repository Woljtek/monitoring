import { SELECTOPTIONS } from 'components/InvalidationEditor/options';
import { SelectCategory } from 'components/InvalidationEditor/types';
import _ from 'lodash';
import { PanelOptions } from 'types';
import { RootCauseEditorOptions } from './types';

export const DEFAULTVALUEROOTCAUSE: RootCauseEditorOptions[] = [];
export const getSelectOptions = (options: PanelOptions): SelectCategory => {
  const selectOptions = _.cloneDeep(SELECTOPTIONS);
  if (
    options?.rootCauseListCompleteness !== undefined &&
    options['rootCauseListCompleteness'].length > 0
  ) {
    selectOptions["COMPLETENESS"]['rootCause']['options'] = options['rootCauseListCompleteness'];
    selectOptions["COMPLETENESS"]['rootCause']['value'] = options['rootCauseListCompleteness'][0].value;
  } else {
    selectOptions["COMPLETENESS"]['rootCause']['options'] = [];
    selectOptions["COMPLETENESS"]['rootCause']['value'] = ' ';
  }
  return selectOptions["COMPLETENESS"];
};
export const SELECTOPTIONSICONS = [
  {
    label: 'Check',
    imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/check.svg',
    value: 'public/plugins/cs-group-invalidations-completeness/img/check.svg',
  },
  {
    label: 'Logo',
    imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/logo.svg',
    value: 'public/plugins/cs-group-invalidations-completeness/img/logo.svg',
  },
  {
    label: 'On-Time',
    imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/on-time.svg',
    value: 'public/plugins/cs-group-invalidations-completeness/img/on-time.svg',
  },
  {
    label: 'Orbit',
    imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/orbit.svg',
    value: 'public/plugins/cs-group-invalidations-completeness/img/orbit.svg',
  },
  {
    label: 'Parabolic-Dishes',
    imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/parabolic-dishes.svg',
    value: 'public/plugins/cs-group-invalidations-completeness/img/parabolic-dishes.svg',
  },
  {
    label: 'Question',
    imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/question.svg',
    value: 'public/plugins/cs-group-invalidations-completeness/img/question.svg',
  },
  {
    label: 'Satellite',
    imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/satellite.svg',
    value: 'public/plugins/cs-group-invalidations-completeness/img/satellite.svg',
  },
  {
    label: 'Settings',
    imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/settings.svg',
    value: 'public/plugins/cs-group-invalidations-completeness/img/settings.svg',
  },
];
