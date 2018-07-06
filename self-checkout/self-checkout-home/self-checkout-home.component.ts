import { IMenuItem } from '../../common/imenuitem';
import { IScreen } from '../../common/iscreen';
import { Component, HostListener } from '@angular/core';
import { SessionService } from '../../core';
import { ObservableMedia } from '@angular/flex-layout';

@Component({
  selector: 'app-self-checkout-home',
  templateUrl: './self-checkout-home.component.html',
  styleUrls: ['./self-checkout-home.component.scss']

})
export class SelfCheckoutHomeComponent implements IScreen {

  screen: any;
  public menuItems: IMenuItem[];
  private actionSent = false;

  constructor(public session: SessionService, public media: ObservableMedia) {
  }

  @HostListener('document:click', ['$event'])
  @HostListener('document:touchstart', ['$event'])
  begin() {
    if (this.menuItems && this.menuItems.length > 0) {
      this.onMenuItemClick(this.menuItems[0]);
    }
  }

  show(screen: any) {
    this.actionSent = false;
    this.screen = screen;
    this.menuItems = screen.menuItems;
  }

  onEnter(value: string) {
    this.session.onAction('Save');
  }

  getClass(): String {
    // return 'main-menu-grid-list';
    return 'foo';
  }

  onMenuItemClick(menuItem: IMenuItem) {
    if (!this.actionSent) {
      this.session.onAction(menuItem, null, menuItem.confirmationMessage);
      this.actionSent = true;
    }
  }
}
