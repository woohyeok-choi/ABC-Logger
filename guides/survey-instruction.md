# Survey JSON Format

## Overall
```perl
{
    # Title of survey
    "title": { 
        "main": string, # Primary text
        "alt": string # Presented when timeout and timeoutAction is set as "ALT_TEXT"; "main" is still presented if this field is empty.
    },

    # Message of survey
    "message": {
        "main": string,
        "alt": string
    },

    # Instruction of survey
    "instruction": {
        "main": string,
        "alt": string,
    },

    # Questions (more details are presented below)
    "question": Array<{
        "title": {
            "main": string,
            "alt": string,
        },
        "isOthersShown": boolean, # "Others" is presented or not at the bottom of the primary question UI.
        "option": {
            "type": "FREE_TEXT" | "RADIO_BUTTON" | "CHECK_BOX" | "DROPDOWN" | "SLIDER" | "RANGE" | "LINEAR_SCALE"
        },
    }>,
    

    # Timeout
    # If timeout is not your concern, set 'amount' as negative (or, timeoutAction as 'NONE')
    "timeout": {
        "amount": integer,
        "unit": "MILLISECOND" | "SECOND" | "MINUTE" | "HOUR" | "DAY",
    },
    
    # Action when timeout occurs
    # - NONE: No action
    # - ALT_TEXT: Alternative text is presented
    # - DISABLED: Make the survey disable to answer
    "timeoutAction": "NONE" | "ALT_TEXT" | "DISABLED",

    # Survey start time (surveys begin to be scheduled after this amount of time)
    # Survey scheduling immediately starts when negative 'amount'.
    "timeFrom": {
        "amount": integer,
        "unit": "MILLISECOND" | "SECOND" | "MINUTE" | "HOUR" | "DAY",
    },

    # Survey end time (survey scheduling is canceled after this amount of time)
    # No end time when negative 'amount'.
    "timeTo": {
        "amount": integer,
        "unit": "MILLISECOND" | "SECOND" | "MINUTE" | "HOUR" | "DAY",
    },

    # Inter-day schedule of survey trigger (more details are presented below)
    "interDaySchedule": {
        "type": "DAILY" | "DAY_OF_WEEK" | "DATE"
    },

    # Intra-day schedule of survey trigger (more details are presented below),
    "intraDaySchedule": {
        "type": "INTERVAL" | "EVENT" | "TIME"
    }
}
```

## Question
### Free text
```perl
{
    "title": {
        "main": string,
        "alt": string,
    },
    "isOthersShown": boolean,
    "option": {
        "type": "FREE_TEXT",
        "isSingleLine": boolean # EditText can be presented as a single line or not.
    }
}
```
### Single choice with radio button
```perl
{
    "title": {
        "main": string,
        "alt": string,
    },
    "isOthersShown": boolean,
    "option": {
        "type": "RADIO_BUTTON",
        "isVertical": boolean # Buttons are aligned as a vertical or horizontal way
        "items": Array<string> # Option items (or buttons)
    }
}
```
### Multiple choice with check boxes
```perl
{
    "title": {
        "main": string,
        "alt": string,
    },
    "isOthersShown": boolean,
    "option": {
        "type": "CHECK_BOX",
        "isVertical": boolean # Buttons are aligned as a vertical or horizontal way
        "items": Array<string> # Option items (or buttons)
    }
}
```

### Single choice with dropdown
```perl
{
    "title": {
        "main": string,
        "alt": string,
    },
    "isOthersShown": boolean,
    "option": {
        "type": "DROPDOWN",
        "items": Array<string> # Option items
    }
}
```

### Number choice with slider
```perl
{
    "title": {
        "main": string,
        "alt": string,
    },
    "isOthersShown": boolean,
    "option": {
        "type": "SLIDER",
        "min": float, # Min. value of slider
        "max": float, # Max. value of slider
        "step": float # Step. value of slider
    }
}
```


### Number-range choice with slider
```perl
{
    "title": {
        "main": string,
        "alt": string,
    },
    "isOthersShown": boolean,
    "option": {
        "type": "RANGE",
        "min": float, # Min. value of slider
        "max": float, # Max. value of slider
        "step": float # Step. value of slider
    }
}
```

### Linear-scale with slider
* Differences from slider:
  * UI is slightly different; there is no highlight color for selection.
  * Default value can be set
  * Min and max label can be set.
```perl
{
    "title": {
        "main": string,
        "alt": string,
    },
    "isOthersShown": boolean,
    "option": {
        "type": "LINEAR_SCALE",
        "min": float, # Min. value of slider
        "max": float, # Max. value of slider
        "step": float # Step. value of slider
        "defaultValue": float, # Default value of slider
        "minLabel": string, # Text presented at the starting point
        "maxLabel": string # Text presented at the end point
    }
}
```

## Inter-day Schedule
* Inter-day schedule determines which days surveys are triggered.

### Daily schedule
* Survey can be triggered everyday except for certain dates.
```perl
{
    "type": "DAILY",
    "exceptDates": Array<{
        "year": integer,
        "month": integer,
        "day": integer
    }>
}
```

### Date schedule
* Survey can be triggered only at certain dates.
```perl
{
    "type": "DATE",
    "dates": Array<{
        "year": integer,
        "month": integer, # Month starts from 1, not 0.
        "day": integer
    }>
}
```

### Day-of-week schedule
* Survey can be triggered only at certain days of week.
```perl
{
    "type": "DAY_OF_WEEK",
    "dates": Array<"MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY">
}
```

## Intra-day Schedule
* Intra-day schedule determines which time surveys are triggered within a day.

### Time schedule
* Survey can be triggered at a certain time.

```perl
{
    "type": "TIME",
    "times": Array<{
        "hour": integer,
        "minute": integer,
        "second": integer
    }>
}
```

### Interval
* Survey can be triggered at a certain interval.

```perl
{
    "type": "INTERVAL",
    # Start time within a day
    "timeFrom": {
        "hour": integer,
        "minute": integer,
        "second": integer
    },
    # End time within a day
    "timeTo": {
        "hour": integer,
        "minute": integer,
        "second": integer
    },
    # Default interval
    "intervalDefault" {
        "amount": integer,
        "unit": "MILLISECOND" | "SECOND" | "MINUTE" | "HOUR" | "DAY",
    },
    # Flex interval; random delay (this amount of time, at maximum) is added.
    # If random delay is not a concern, set "amount" as a negative integer.
    "intervalFlex" {
        "amount": integer,
        "unit": "MILLISECOND" | "SECOND" | "MINUTE" | "HOUR" | "DAY",
    },
}
```

### Event
* Survey can be triggered/canceled at a certain event after "delayDefault" + "delayFlex".

```perl
{
    "type": "EVENT",
    # Start time within a day
    "timeFrom": {
        "hour": integer,
        "minute": integer,
        "second": integer
    },
    # End time within a day
    "timeTo": {
        "hour": integer,
        "minute": integer,
        "second": integer
    },
    # Default 
    "delayDefault" {
        "amount": integer,
        "unit": "MILLISECOND" | "SECOND" | "MINUTE" | "HOUR" | "DAY",
    },
    # Flex delay; random delay (this amount of time, at maximum) is added.
    # If random delay is not a concern, set "amount" as a negative integer.
    "delayFlex" {
        "amount": integer,
        "unit": "MILLISECOND" | "SECOND" | "MINUTE" | "HOUR" | "DAY",
    },
    "eventsTrigger": Array<string>,
    "eventsCanceled": Array<string>,
}
```

## Example
```json
{
	"title": {
		"main": "title_main",
		"alt": "title_alt"
	},
	"message": {
		"main": "message_main",
		"alt": "message_alt"
	},
	"instruction": {
		"main": "instruction_main",
		"alt": "instruction_alt"
	},
	"question": [
		{
			"title": {
				"main": "question_title",
				"alt": "question_alt"
			},
			"isOtherShown": true,
			"option": {
				"type": "FREE_TEXT",
				"isSingleLine": true
			}
		},
		{
			"title": {
				"main": "question_title",
				"alt": "question_alt"
			},
			"isOtherShown": true,
			"option": {
				"type": "RADIO_BUTTON",
				"isVertical": true,
				"items": [
					"A",
					"B",
					"C"
				]
			}
		},
		{
			"title": {
				"main": "question_title",
				"alt": "question_alt"
			},
			"isOtherShown": true,
			"option": {
				"type": "CHECK_BOX",
				"isVertical": true,
				"items": [
					"A",
					"B",
					"C"
				]
			}
		},
		{
			"title": {
				"main": "question_title",
				"alt": "question_alt"
			},
			"isOtherShown": true,
			"option": {
				"type": "FREE_TEXT",
				"isSingleLine": false
			}
		},
		{
			"title": {
				"main": "question_title",
				"alt": "question_alt"
			},
			"isOtherShown": true,
			"option": {
				"type": "RADIO_BUTTON",
				"isVertical": false,
				"items": [
					"A",
					"B",
					"C"
				]
			}
		},
		{
			"title": {
				"main": "question_title",
				"alt": "question_alt"
			},
			"isOtherShown": true,
			"option": {
				"type": "CHECK_BOX",
				"isVertical": false,
				"items":  [
					"A",
					"B",
					"C"
				]
			}
		},
		{
			"title": {
				"main": "question_title",
				"alt": "question_alt"
			},
			"isOtherShown": true,
			"option": {
				"type": "DROPDOWN",
				"items": [
					"A",
					"B",
					"C"
				]
			}
		},
		{
			"title": {
				"main": "question_title",
				"alt": "question_alt"
			},
			"isOtherShown": true,
			"option": {
				"type": "SLIDER",
				"min": 0.0,
				"max": 100.0,
				"step": 1.0
			}
		},
		{
			"title": {
				"main": "question_title",
				"alt": "question_alt"
			},
			"isOtherShown": true,
			"option": {
				"type": "RANGE",
				"min": 0.0,
				"max": 100.0,
				"step": 1.0
			}
		},
		{
			"title": {
				"main": "question_title",
				"alt": "question_alt"
			},
			"isOtherShown": true,
			"option": {
				"type": "LINEAR_SCALE",
				"min": 0.0,
				"max": 100.0,
				"step": 1.0,
				"defaultValue": 50.0,
				"minLabel": "Less",
				"maxLabel": "Highly"
			}
		}
	],
	"timeout": {
		"amount": 1,
		"unit": "HOUR"
	},
	"timeoutAction": "ALT_TEXT",
	"timeFrom": {
		"amount": 5,
		"unit": "HOUR"
	},
	"timeTo": {
		"amount": 5,
		"unit": "HOUR"
	},
	"interDaySchedule": {
		"type": "DAILY",
		"exceptDates": [
			{
				"year": 2005,
				"month": 5,
				"day": 3
			},
			{
				"year": 2005,
				"month": 5,
				"day": 4
			},
			{
				"year": 2005,
				"month": 5,
				"day": 5
			}
		]
	},
	"intraDaySchedule": {
		"type": "INTERVAL",
		"timeFrom": {
			"hour": 10,
			"minute": 11,
			"second": 12
		},
		"timeTo": {
			"hour": 15,
			"minute": 11,
			"second": 12
		},
		"intervalDefault": {
			"amount": 5,
			"unit": "HOUR"
		},
		"intervalFlex": {
			"amount": 30,
			"unit": "SECOND"
		}
	}
}
```