import { Component } from '@angular/core';
import { PosScreen } from '../pos-screen/pos-screen.component';
import { IActionItem } from '../../core/interfaces/action-item.interface';
import { OpenposMediaService } from '../../core/services/openpos-media.service';
import { Observable } from 'rxjs';

/**
 * @ignore
 */
@Component({
  selector: 'app-item-options',
  templateUrl: './item-options.component.html',
  styleUrls: ['./item-options.component.scss']
})
export class ItemOptionsComponent extends PosScreen<any> {

  selectedImage: String;
  isMobile: Observable<boolean>;

  constructor(private mediaService: OpenposMediaService) {
    super();
    this.isMobile = mediaService.mediaObservableFromMap(new Map([
      ['xs', true],
      ['sm', false],
      ['md', false],
      ['lg', false],
      ['xl', false]
    ]));
  }

  buildScreen() {
    if (this.screen.imageUrls) {
      this.selectedImage = this.screen.imageUrls[0];
    }
  }

  public doMenuItemAction(menuItem: IActionItem) {
    this.session.onAction(menuItem);
  }

  public selectImage(imageUrl: String) {
    this.selectedImage = imageUrl;
  }

}
