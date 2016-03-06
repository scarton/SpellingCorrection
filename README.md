# SpellingCorrection
Probablistic spelling correction in Java, based on work by Norvic: http://norvig.com/spell-correct.html

Two classes. One to build a model, the other to perform spelling correction on a list of one or more words. Each has a main method.

#Building a Model
There is a prebuilt model in resources (spelling.model.gz). By default, the spelling corrector looks for this model on the classpath, and this is the default model it will use. It was built from Norvic's big.txt file.

Build a new model as follows:

```
java -cp SpellingCorrection-1.0.jar SpellingModelBuilder large_text_file.txt model_name
```
    
The model_name defaults to spelling.model creating a model file named spelling.model.gz.

#Correcting Spelling
The Spelling class performs spell correction. It has a main class which will take all of the arguments on the 
command line and correct them individually as words.

```    
java -cp SpellingCorrection-1.0.jar Spelling teh quic borwn fxo jumpde voer the lazyz dogg
```

Using the spelling corrector in code:
```
        Spelling spelling = new Spelling();
        spelling.loadModels();
```
If the model you are using is named differently than the default (spelling.model), you can specify it:
```
        spelling.setMODEL_NAME("my_model_name");
```
The named model can either be a file or a classpath resource. The model loader checks to see if the specified model 
exists as a file, and if so, loads that file. If not, it looks for the model on the classpath. In a little 
weirdness, if loading from a file, the full file name and path is expected. If loading from the classpath, 
the name only is expected - the code appends .gz as the model is expected to be a gzipped file. 

A feature of this version of the spell corrector is the use of two models. An "industry-specific" model 
can be specified and will be used first for correction. The general model will be used second. This
feature is to solve a specific class of problems: in the general case, ankel would be corrected to angel, as that is more probably the word meant. But by using a (perhaps) medical model, ankel would be corrected to ankle.
```
        spelling.setINDUSTRY_MODEL_NAME("specialized_model_name");
```
Setting model names must be called prior to loading the models. Also, if you specify only the industry model, the code will still load the default model as well. If your intent is to only have one model, but different than the default, specify that in setMODEL_NAME.




 


