@import org.springframework.samples.petclinic.system.Templating.CustomFields
@import org.springframework.validation.FieldError
@param CustomFields fields
@param String label
@param String name
@param String value
@param java.util.List<String> items

!{var valid = fields.hasErrors(name);}
<div class="${"form-group" + (valid ? "" : " has-error")} form-group">
    <label class="col-sm-2 control-label">${label}</label>

    <div class="col-sm-10">
      <select id="${name}" name="${name}" value="${value}">
          @for(String item : items)
              <option value="${item}">${item}</option>
          @endfor
      </select>

        @if(valid)
            <span class="fa fa-ok form-control-feedback"
                  aria-hidden="true"></span>
        @else
            <span
                    class="fa fa-remove form-control-feedback"
                    aria-hidden="true"></span>
            @for(FieldError error : fields.getFieldErrors(name))
                <span class="help-inline">${error.toString()}</span>
            @endfor
        @endif
    </div>
</div>
