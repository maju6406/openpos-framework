import { ScreenService } from './../screen.service';
import { AbstractApp } from '../screens/abstract-app';
import { IMenuItem } from '../common/imenuitem';
import { Component, DoCheck } from '@angular/core';
import { SessionService } from '../session.service';
import { FocusDirective } from '../common/focus.directive';
import { MdDialog, MdDialogRef } from '@angular/material';
import { IconService } from './../icon.service';

@Component({
  selector: 'app-pos',
  templateUrl: './pos.component.html'
})
export class PosComponent extends AbstractApp implements DoCheck {

  // TODO these should be conflated or removed perhaps.
  public menuItems: IMenuItem[] = [];
  public menuActions: IMenuItem[] = [];
  public backButton: IMenuItem;
  public isCollapsed = true;

  constructor(screenService: ScreenService, public session: SessionService, public dialog: MdDialog, iconService: IconService) {
    super(screenService, session, dialog, iconService);
  }

  protected appName(): String {
    return 'pos';
  }

  ngDoCheck(): void {
    if (typeof this.session.screen !== 'undefined') {
      this.menuItems = this.session.screen.menuItems;
      this.menuActions = this.session.screen.menuActions;
      this.backButton = this.session.screen.backButton;
      if (!this.menuActions || this.menuActions.length === 0) {
        this.isCollapsed = true;
      }
    }

    super.ngDoCheck();
  }

}
