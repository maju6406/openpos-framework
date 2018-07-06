import { FormGroup, FormControl, ValidatorFn, Validators } from '@angular/forms';
import { Component, AfterViewInit, OnInit } from '@angular/core';
import { ValidatorsService } from '../../core';
import { PosScreen } from '../pos-screen/pos-screen.component';

@Component({
  selector: 'app-prompt',
  templateUrl: './prompt.component.html'
})
export class PromptComponent extends PosScreen<any> implements AfterViewInit, OnInit {
  initialized = false;
  promptFormGroup: FormGroup;

  constructor( private validatorsService: ValidatorsService) {
      super();
  }

  buildScreen() {
  }

  ngOnInit(): void {

    const group: any = {};
    const validators: ValidatorFn[] = [];
    validators.push(Validators.required);
    validators.push(this.validatorsService.getValidator(this.screen.responseType));

    if (this.screen.minLength) {
      validators.push(Validators.minLength(this.screen.minLength));
    }

    if (this.screen.maxLength) {
      validators.push(Validators.maxLength(this.screen.maxLength));
    }

    group['promptInputControl'] = new FormControl(this.screen.responseText, validators);
    // When showing a DATE, there is also a hidden field to handle picking of dates using
    // a date picker, need to add a FormControl for that also.
    if (this.screen.responseType && this.screen.responseType.toLowerCase().indexOf('date') >= 0) {
      group['promptInputHiddenDateControl'] = new FormControl();
    }
    this.promptFormGroup = new FormGroup(group);
  }

  ngAfterViewInit(): void {
    this.initialized = true;
  }

  onFormSubmit(): void {
    if (this.promptFormGroup.valid) {
      const payload = this.promptFormGroup.value['promptInputControl'];
      if (this.screen.actionButton) {
        this.session.onAction(this.screen.actionButton.action, payload);
      } else {
        this.session.onAction(this.screen.action, payload);
      }
    }
  }

}
