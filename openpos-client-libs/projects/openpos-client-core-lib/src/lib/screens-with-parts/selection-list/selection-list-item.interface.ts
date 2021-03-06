import { SelectionListItemDisplayProperty } from './selection-list-item-display-property.interface';

export interface ISelectionListItem {
    title: string;
    properties: SelectionListItemDisplayProperty[];
    selected: boolean;
    enabled: boolean;
    itemImageUrl: string;
}
