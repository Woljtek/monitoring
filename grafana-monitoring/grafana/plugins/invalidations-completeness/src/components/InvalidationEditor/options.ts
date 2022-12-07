import { Responsibility, SelectOptionsInterface } from './types';

export const CATEGORYOPTIONS = [
  {
    label: Responsibility.PDGS,
    value: Responsibility.PDGS,
    description: 'Invalidation due to sensing root cause',
    imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/orbit.svg',
  },
  {
    label: Responsibility.E2E,
    value: Responsibility.E2E,
    description: 'Invalidation due to antenna root cause',
    imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/parabolic-dishes.svg',
  },
];

export const IMPACTOPTIONS = [
  {
    label: 'Completeness',
    value: 'COMPLETENESS',
    description: 'Invalidation has implication on completeness',
    imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/check.svg',
  },
  {
    label: 'Timeliness',
    value: 'TIMELINESS',
    description: 'Invalidation has implication on timeliness',
    imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/on-time.svg',
  },
];

export const COMPLETENESSOPTIONS = [
  {
    label: 'MPS',
    value: 'MPS',
    description: 'MPS - network issue',
    imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/orbit.svg',
  },
  {
    label: 'SGS',
    value: 'SGS',
    description: 'SGS - network issue',
    imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/settings.svg',
  },
  {
    label: 'MTI',
    value: 'MTI',
    description: 'MTI - network issue',
    imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/settings.svg',
  },
  {
    label: 'NSG',
    value: 'NSG',
    description: 'NSG - network issue',
    imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/settings.svg',
  },
  {
    label: 'EDRS-A',
    value: 'EDRS-A',
    description: 'EDRS-A - network issue',
    imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/settings.svg',
  },
  {
    label: 'PDGS',
    value: 'PDGS',
    description: 'PDGS - network issue',
    imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/question.svg',
  },
  {
    label: 'PDGS',
    value: 'PDGS',
    description: 'PDGS - other',
    imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/question.svg',
  },
]
export const SELECTOPTIONS: SelectOptionsInterface = {
  COMPLETENESS: {
    rootCause: {
      disabled: false,
      value: 'S1A',
      options: [
        {
          label: 'S1A',
          value: 'S1A',
          description: 'S1A - telemetry quality',
          imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/satellite.svg',
        },
        {
          label: 'S1B',
          value: 'S1B',
          description: 'S1B - telemetry quality',
          imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/orbit.svg',
        },
        {
          label: 'MPS',
          value: 'MPS',
          description: 'MPS - telemetry quality',
          imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/orbit.svg',
        },
        {
          label: 'SGS',
          value: 'SGS',
          description: 'SGS - telemetry quality',
          imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/settings.svg',
        },
        {
          label: 'MTI',
          value: 'MTI',
          description: 'MTI - telemetry quality',
          imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/settings.svg',
        },
        {
          label: 'NSG',
          value: 'NSG',
          description: 'NSG - telemetry quality',
          imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/settings.svg',
        },
        {
          label: 'EDRS-A',
          value: 'EDRS-A',
          description: 'EDRS-A - telemetry quality',
          imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/settings.svg',
        },
        {
          label: 'CFI',
          value: 'CFI',
          description: 'CFI',
          imgUrl: 'public/cs-group-invalidations-completeness/img/settings.svg',
        },
        {
          label: 'PDGS',
          value: 'PDGS',
          description: 'PDGS',
          imgUrl: 'public/plugins/cs-group-invalidations-completeness/img/question.svg',
        },
      ],
    },
  },
};
