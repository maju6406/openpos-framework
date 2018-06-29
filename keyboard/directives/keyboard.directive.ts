import { Subscription } from 'rxjs';
import { Directive, ElementRef, EventEmitter, HostListener, Input, OnDestroy, Optional, Output, Self } from '@angular/core';
import { MatKeyboardComponent } from '../components/keyboard/keyboard.component';
import { MatKeyboardRef } from '../classes/keyboard-ref.class';
import { MatKeyboardService } from '../services/keyboard.service';
import { NgControl } from '@angular/forms';
import { SessionService } from '../../services/session.service';
import { Configuration } from '../../configuration/configuration';

@Directive({
    // tslint:disable-next-line:directive-selector
    selector: 'input:not([type=checkbox]), textarea'
})
export class KeyboardDirective implements OnDestroy {

    private _keyboardRef: MatKeyboardRef<MatKeyboardComponent>;

    private screen: any;

    private screenSubscription: Subscription;

    @Input() keyboardLayout: string;

    @Input() darkTheme: boolean;

    @Input() duration: number;

    @Input() isDebug: boolean;

    @Output() enterClick: EventEmitter<void> = new EventEmitter<void>();

    @Output() capsClick: EventEmitter<void> = new EventEmitter<void>();

    @Output() altClick: EventEmitter<void> = new EventEmitter<void>();

    @Output() shiftClick: EventEmitter<void> = new EventEmitter<void>();

    constructor(public session: SessionService, private _elementRef: ElementRef,
        private _keyboardService: MatKeyboardService,
        @Optional() @Self() private _control?: NgControl) {
        this.screenSubscription = this.session.subscribeForScreenUpdates((screen: any): void => this.screen = screen);
    }

    ngOnDestroy() {
        this._hideKeyboard();
        this.screenSubscription.unsubscribe();
    }

    @HostListener('focus', ['$event'])
    private _showKeyboard() {
        if (Configuration.useOnScreenKeyboard) {

            this._keyboardRef = this._keyboardService.open(this.keyboardLayout, {
                darkTheme: true,
                duration: this.duration,
                isDebug: this.isDebug
            });

            // reference the input element
            this._keyboardRef.instance.setInputInstance(this._elementRef);

            // connect outputs
            this._keyboardRef.instance.enterClick.subscribe(() => {
                const event = new Event('submit', { cancelable: true, bubbles: true });
                if (this._elementRef.nativeElement.form) {
                    this._elementRef.nativeElement.form.dispatchEvent(event);
                }
                const enterPressedEvent = new KeyboardEvent('keypress', { key: 'Enter', code: 'Enter', cancelable: true, bubbles: true });
                this._elementRef.nativeElement.dispatchEvent(enterPressedEvent);
                const enterDownEvent = new KeyboardEvent('keydown', { key: 'Enter', code: 'Enter', cancelable: true, bubbles: true });
                this._elementRef.nativeElement.dispatchEvent(enterDownEvent);
                const enterUpEvent = new KeyboardEvent('keyup', { key: 'Enter', code: 'Enter', cancelable: true, bubbles: true });
                this._elementRef.nativeElement.dispatchEvent(enterUpEvent);

                this.enterClick.next();
            });
            this._keyboardRef.instance.capsClick.subscribe(() => this.capsClick.next());
            this._keyboardRef.instance.altClick.subscribe(() => this.altClick.next());
            this._keyboardRef.instance.shiftClick.subscribe(() => this.shiftClick.next());
        }
    }

    @HostListener('blur', ['$event'])
    private _hideKeyboard() {
        if (this._keyboardRef) {
            this._keyboardRef.dismiss();
        }
    }

}
