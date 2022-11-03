import { RootCauseEditorOptions } from 'components/RootCauseEditor/types';
import { SelectOptionsInterface } from './types';

export const DEFAULTSENSINGROOTCAUSES: RootCauseEditorOptions[] = [
  {
    label: 'S1A',
    value: 'S1A',
    description: 'Rootcause is S1A satellite',
    imgUrl: 'public/plugins/invalidations/img/satellite.svg',
  },
  {
    label: 'S1B',
    value: 'S1B',
    description: 'Rootcause is S1B satellite',
    imgUrl: 'public/plugins/invalidations/img/orbit.svg',
  },
  {
    label: 'PDGS - Mission Planning',
    value: 'PDGS',
    description: 'Rootcause is PDGS or Mission Planning',
    imgUrl: 'public/plugins/invalidations/img/settings.svg',
  },
  {
    label: 'Unknown',
    value: undefined,
    description: 'Rootcause is unknown',
    imgUrl: 'public/plugins/invalidations/img/question.svg',
  },
];

export const DEFAULTDOWNLINKROOTCAUSES: RootCauseEditorOptions[] = [
  {
    label: 'MPS',
    value: 'MPS',
    description: 'Rootcause is MPS station',
    imgUrl: 'public/plugins/invalidations/img/parabolic-dishes.svg',
  },
  {
    label: 'SGS',
    value: 'SGS',
    description: 'Rootcause is SGS station',
    imgUrl: 'public/plugins/invalidations/img/parabolic-dishes.svg',
  },
  {
    label: 'MTI',
    value: 'MTI',
    description: 'Rootcause is MTI station',
    imgUrl: 'public/plugins/invalidations/img/parabolic-dishes.svg',
  },
  {
    label: 'EDRS-A',
    value: 'EDRSA',
    description: 'Rootcause is EDRS-A satellite',
    imgUrl: 'public/plugins/invalidations/img/orbit.svg',
  },
  {
    label: 'PDGS - Mission Planning',
    value: 'PDGS',
    description: 'Rootcause is PDGS or Mission Planning',
    imgUrl: 'public/plugins/invalidations/img/settings.svg',
  },

  {
    label: 'Unknown',
    value: undefined,
    description: 'Rootcause is unknown',
    imgUrl: 'public/plugins/invalidations/img/question.svg',
  },
];
export const CATEGORYOPTIONS = [
  {
    label: 'Sensing',
    value: 'SENSING',
    description: 'Invalidation due to sensing root cause',
    imgUrl: 'public/plugins/invalidations/img/orbit.svg',
  },
  {
    label: 'Downlink',
    value: 'DOWNLINK',
    description: 'Invalidation due to antenna root cause',
    imgUrl: 'public/plugins/invalidations/img/parabolic-dishes.svg',
  },
];

export const SELECTOPTIONS: SelectOptionsInterface = {
  SENSING: {
    impact: {
      disabled: true,
      value: 'COMPLETENESS',
      options: [
        {
          label: 'Completeness',
          value: 'COMPLETENESS',
          description: 'Invalidation has implication on completeness',
          imgUrl: 'public/plugins/invalidations/img/check.svg',
        },
      ],
    },
    rootCause: {
      disabled: false,
      value: DEFAULTSENSINGROOTCAUSES[0].value,
      options: DEFAULTSENSINGROOTCAUSES,
    },
  },
  DOWNLINK: {
    impact: {
      disabled: false,
      value: 'COMPLETENESS',
      options: [
        {
          label: 'Completeness',
          value: 'COMPLETENESS',
          description: 'Invalidation has implication on completeness',
          imgUrl: 'public/plugins/invalidations/img/check.svg',
        },
        {
          label: 'Timeliness',
          value: 'TIMELINESS',
          description: 'Invalidation has implication on timeliness',
          imgUrl: 'public/plugins/invalidations/img/on-time.svg',
        },
      ],
    },
    rootCause: {
      disabled: false,
      value: DEFAULTDOWNLINKROOTCAUSES[0].value,
      options: DEFAULTDOWNLINKROOTCAUSES,
    },
  },
};
