import { IconService } from './../icon.service';
import { DomSanitizer } from '@angular/platform-browser';
import { ScreenDirective } from '../common/screen.directive';
import { IScreen } from '../common/iscreen';
import { ScreenService } from './../screen.service';
import { DialogComponent } from '../screens/dialog.component';
import { IMenuItem } from '../common/imenuitem';
import { Component, OnInit, OnDestroy, DoCheck } from '@angular/core';
import { Type, ViewChild, ComponentFactory } from '@angular/core';
import { SessionService } from '../session.service';
import { StatusBarComponent } from '../screens/statusbar.component';
import { FocusDirective } from '../common/focus.directive';
import { MdDialog, MdDialogRef, MdIconRegistry } from '@angular/material';

export abstract class AbstractApp implements OnInit, OnDestroy, DoCheck {

    private dialogRef: MdDialogRef<DialogComponent>;

    private previousScreenType: string;

    private previousScreenSequenceNumber: number;

    @ViewChild(ScreenDirective) host: ScreenDirective;

    constructor(private screenService: ScreenService,
        public session: SessionService, public dialog: MdDialog,
        public iconService: IconService) {
    }

    protected abstract appName(): String;

    ngOnInit(): void {
        this.session.unsubscribe();
        this.session.subscribe(this.appName());
        this.iconService.registerLocalSvgIcons();
    }

    ngOnDestroy(): void {
        this.session.unsubscribe();
    }

    ngDoCheck(): void {
        if (this.session.dialog && !this.dialogRef) {
            setTimeout(() => this.openDialog(), 0);
        } else if (!this.session.dialog && this.dialogRef) {
            console.log('closing dialog');
            this.dialogRef.close();
            this.dialogRef = null;
        }

        let screen: IScreen = null;
        if (this.session.screen &&
            ((this.session.screen.sequenceNumber !== this.previousScreenSequenceNumber && this.session.screen.refreshAlways)
                || this.session.screen.type !== this.previousScreenType)) {
            console.log(`Switching screens from ${this.previousScreenType} to ${this.session.screen.type}`);
            const componentFactory: ComponentFactory<IScreen> = this.screenService.resolveScreen(this.session.screen.type);
            const viewContainerRef = this.host.viewContainerRef;
            viewContainerRef.clear();
            screen = viewContainerRef.createComponent(componentFactory).instance;
            this.previousScreenType = this.session.screen.type;
            screen.show(this.session);
            this.previousScreenSequenceNumber = this.session.screen.sequenceNumber;
        }

    }

    openDialog() {
        this.dialogRef = this.dialog.open(DialogComponent, { disableClose: true });
        this.dialogRef.afterClosed().subscribe(result => {
            if (result) {
                this.session.onAction(result);
                this.dialogRef = null;
            }
        });
    }
}
