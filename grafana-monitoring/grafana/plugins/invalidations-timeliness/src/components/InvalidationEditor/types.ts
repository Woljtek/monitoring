import { SelectableValue } from '@grafana/data';

export const Responsibility = {
  E2E: 'E2E',
  PDGS: 'PDGS',
} as const;
export type EResponsibility = typeof Responsibility[keyof typeof Responsibility];

export interface FormDTO {
  responsibility: SelectableValue<EResponsibility>;
  rootCause: SelectableValue<string>;
  label: string;
  comment: string;
  anomalyIdentifier: number
}

export interface SelectOptionsInterface {
  [key: string]: {
    [key: string]: {
      disabled: boolean;
      value: string | undefined;
      options: Array<SelectableValue<string>>;
    };
  };
}

export interface SelectCategory {
  [key: string]: {
    disabled: boolean;
    value: string | undefined;
    options: Array<SelectableValue<string>>;
  };
}
