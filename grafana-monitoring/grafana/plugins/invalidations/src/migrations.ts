import { PanelModel } from '@grafana/data';
import { PanelOptions } from './types';

/**
 * This is called when the panel changes from another panel
 */
export const tablePanelChangedHandler = (
  panel: PanelModel<Partial<PanelOptions>> | any,
  prevPluginId: string,
  prevOptions: any
) => {
  return {};
};
