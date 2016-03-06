# SpellingCorrection
Probablistic spelling correction in Java, based on work by Norvic: http://norvig.com/spell-correct.html

Two classes. One to build a model, the other to perform spelling correction on a list of one or more words. Each has a main method.

#Building a Model
There is a prebuilt model in resources (spelling.model.gz). The spelling corrector looks for a model on the classpath, and this is the default model it will use. It was built from Norvic's big.txt file.

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


 


