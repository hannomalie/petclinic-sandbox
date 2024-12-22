@import org.springframework.samples.petclinic.owner.PetType
@import org.springframework.samples.petclinic.system.Templating.CustomFields
@import org.springframework.validation.FieldError
@param CustomFields fields
@param String label
@param String name
@param String value
@param String type

!{var valid = fields.hasErrors(name);}
<div class="${"form-group" + (valid ? "" : " has-error")}">
    <label class="col-sm-2 control-label">${label}</label>
    <div class="col-sm-10">
        @if(type.equals("text"))
            <input class="form-control" type="text" id="${name}" name="${name}" value="${value}" />
        @elseif(type.equals("date"))
            <input class="form-control" type="date" id="${name}" name="${name}" value="${value}"/>
        @endif
        @if(valid)
            <span class="fa fa-ok form-control-feedback" aria-hidden="true"></span>
        @else
            <span class="fa fa-remove form-control-feedback" aria-hidden="true"></span>
            @for(FieldError error : fields.getFieldErrors(name))
                <span class="help-inline">${error.toString()}</span>
            @endfor
        @endif
    </div>
</div>
